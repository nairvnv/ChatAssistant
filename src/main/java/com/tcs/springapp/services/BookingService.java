package com.tcs.springapp.services;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.springframework.stereotype.Service;

import javax.print.Doc;

// Annotation
@Service
public class BookingService {
    static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    static MongoDatabase database = client.getDatabase("ChatApplication_TCS");
    static MongoCollection<Document> reservations = database.getCollection("Reservations");
    static MongoCollection<Document> cancelled_reservations = database.getCollection("CancelledReservations");

    public static Document getStatus(int bookingId)
    {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", bookingId);

        Document doc = reservations.find(whereQuery).first();
        if (doc == null) {
            System.out.println("No results found.");
            doc = new Document("error","No booking details found for the given id");
        } else {
            System.out.println(doc.toJson());
        }
        return(doc);
    }

    public static String cancel(int bookingId)
    {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", bookingId);

        Document doc = reservations.find(whereQuery).first();
        if (doc == null) {
            System.out.println("No Booking found to cancel.");
            return("no booking found to cancel");
        } else {
            reservations.deleteOne(whereQuery);
            try {
                InsertOneResult result = cancelled_reservations.insertOne(doc);
                System.out.println("Success! Inserted document id: " + result.getInsertedId());
            } catch (MongoException me) {
                System.err.println("Unable to insert message in collection due to an error: " + me);
                return("Cancellation failed");
            }

            System.out.println(doc.toJson());
        }
        return("Cancel Successful");
    }
}

