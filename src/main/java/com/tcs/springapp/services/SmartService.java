package com.tcs.springapp.services;

import com.tcs.springapp.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;

@Service
public class SmartService {
    private static Map<String, String> questionAnswer = new HashMap<>();
    static {
        questionAnswer.put("default", "Sorry, I do not understand please chat with a representative to know more");
        questionAnswer.put("greeting", "Hello, what can I do for you today?");
        questionAnswer.put("product-inquiry", "At IHG® Hotels & Resorts, our family of brands offers you a world of choices. From everyday essential travel to luxury escapes and budget-wise staycations, find an ever-growing collection of welcoming experiences.");
        questionAnswer.put("price-inquiry", "Please visit https://www.ihg.com/hotels/us/en/reservation and choose dates and destination to get price details ");
        questionAnswer.put("conversation-continue", "What else can I help you with?");
        questionAnswer.put("conversation-complete", "Enjoy your experience at IHG. Until next time.");
        questionAnswer.put("other", "Please click on other to chat with a representative or navigate our website for more details");
        questionAnswer.put("reward-program", "visit https://www.ihg.com/onerewards/content/us/en/home#scmisc=nav_home_6c  to learn more");
    }

    private static String[] breakSentences(String data) throws FileNotFoundException, IOException {

        try (InputStream modelIn = new FileInputStream("en-sent.bin")) {

            SentenceDetectorME myCategorizer = new SentenceDetectorME(new SentenceModel(modelIn));

            String[] sentences = myCategorizer.sentDetect(data);
//            System.out.println("Sentence Detection: " + Arrays.stream(sentences).collect(Collectors.joining(" | ")));

            return sentences;
        }
    }

    private static String detectCategory(DoccatModel model, String[] finalTokens) throws IOException {

        // Initialize document categorizer tool
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

        // Get best possible category.
        double[] probabilitiesOfOutcomes = myCategorizer.categorize(finalTokens);
        String category = myCategorizer.getBestCategory(probabilitiesOfOutcomes);
//        System.out.println("Category: " + category);

        return category;

    }
    /**
     * Break sentence into words & punctuation marks using tokenizer feature of
     * Apache OpenNLP.
     *
     * @param sentence
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static String[] tokenizeSentence(String sentence) throws FileNotFoundException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream("en-token.bin")) {
            // Initialize tokenizer tool
            TokenizerME myCategorizer = new TokenizerME(new TokenizerModel(modelIn));

            // Tokenize sentence.
            String[] tokens = myCategorizer.tokenize(sentence);
//            System.out.println("Tokenizer : " + Arrays.stream(tokens).collect(Collectors.joining(" | ")));

            return tokens;
        }
    }


    private static String[] detectPOSTags(String[] tokens) throws IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream("en-pos-maxent.bin")) {
            // Initialize POS tagger tool
            POSTaggerME myCategorizer = new POSTaggerME(new POSModel(modelIn));

            // Tag sentence.
            String[] posTokens = myCategorizer.tag(tokens);
//            System.out.println("POS Tags : " + Arrays.stream(posTokens).collect(Collectors.joining(" | ")));

            return posTokens;
        }
    }


    private static String[] lemmatizeTokens(String[] tokens, String[] posTags)
            throws InvalidFormatException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream("en-lemmatizer.bin")) {

            // Tag sentence.
            LemmatizerME myCategorizer = new LemmatizerME(new LemmatizerModel(modelIn));
            String[] lemmaTokens = myCategorizer.lemmatize(tokens, posTags);
//            System.out.println("Lemmatizer : " + Arrays.stream(lemmaTokens).collect(Collectors.joining(" | ")));

            return lemmaTokens;
        }
    }


    private static DoccatModel trainCategorizerModel() throws FileNotFoundException, IOException {
        // faq-categorizer.txt is a custom training data with categories as per our chat
        // requirements.
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File("faq-categorizer.txt"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

        DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });

        TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
        params.put(TrainingParameters.CUTOFF_PARAM, 0);

        // Train a model with classifications from above file.
        DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
        return model;
    }


    public static ChatMessage smartResponse(ChatMessage chatMessage) throws IOException {

        DoccatModel model = trainCategorizerModel();
        String[] sentences = breakSentences(chatMessage.getContent());

        String answer = "";
        boolean conversationComplete = false;

        // Loop through sentences.
        for (String sentence : sentences) {

            // Separate words from each sentence using tokenizer.
            String[] tokens = tokenizeSentence(sentence);

            // Tag separated words with POS tags to understand their gramatical structure.
            String[] posTags = detectPOSTags(tokens);

            // Lemmatize each word so that its easy to categorize.
            String[] lemmas = lemmatizeTokens(tokens, posTags);

            // Determine BEST category using lemmatized tokens used a mode that we trained
            // at start.
            String category = detectCategory(model, lemmas);

            // Get predefined answer from given category & add to answer.
            answer = answer + " " + questionAnswer.get(category);

            // If category conversation-complete, we will end chat conversation.
            if ("conversation-complete".equals(category)) {
                conversationComplete = true;
            }
        }

        // Print answer back to user. If conversation is marked as complete, then end
        // loop & program.
//        System.out.println("##### Chat Bot: " + answer);

        ChatMessage chatResponse = new ChatMessage();
        chatResponse.setType(ChatMessage.MessageType.valueOf("CHAT"));
        chatResponse.setSender("IHG Assistant");
        chatResponse.setContent(answer);

//        System.out.println(answer);
        return chatResponse;

    }

}
