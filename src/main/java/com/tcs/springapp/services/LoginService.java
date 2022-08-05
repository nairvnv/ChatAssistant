package com.tcs.springapp.services;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Service
public class LoginService {
    static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    static MongoDatabase database = client.getDatabase("ChatApplication_TCS");
    static MongoCollection<Document> list_AvailableServices = database.getCollection("List_AvailableServices");
    static MongoCollection<Document> serviceDetails = database.getCollection("ServiceDetails");

    public static Document login(int uid){
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", uid);

        Document doc = list_AvailableServices.find(whereQuery).first();
        List<Document> services = (List<Document>)doc.get("available_Services");

        if (doc == null) {
            System.out.println("No user found.");
            doc = new Document("status",false);
        } else {
            List<Document> slist = new ArrayList<>();
            ListIterator<Document> serviceIterator =services.listIterator();
            while (serviceIterator.hasNext()) {
                Integer s_id = Integer.valueOf(String.valueOf(serviceIterator.next()));
                BasicDBObject serviceQuery = new BasicDBObject();
                serviceQuery.put("_id", s_id);
                slist.add(serviceDetails.find(serviceQuery).first());
            }
            doc.remove("available_Services");
            doc.append("services",slist);
            doc.append("status",true);
            System.out.println(doc.toJson());
        }
        return(doc);
    }
}
