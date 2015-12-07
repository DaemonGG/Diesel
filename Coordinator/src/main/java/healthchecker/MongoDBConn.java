package healthchecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MongoDBConn {

	
    public static boolean insertNonsense(String ip){
    	
    	MongoClient mongoClient = new MongoClient( ip , 27017 );
    	DB db = mongoClient.getDB("diesel");
    	DBCollection job_coll = db.getCollection("testConn");
    	
    	BasicDBObject job = new BasicDBObject("user","I am nothing").append("jobID", "-1-1-1").append("url", "")
        .append("status","0");
    	try{
    		job_coll.insert(job, WriteConcern.SAFE);
    		job_coll.remove(job);
    		return true;
    	}catch(Exception e){
    		return false;
    	}
    }
    
    public void insert(String empname, String filename, DBCollection collection)
    {
        try
        {
            File imageFile = new File(filename);
            FileInputStream f = new FileInputStream(imageFile);
 
            byte b[] = new byte[f.available()];
            f.read(b);
 
            Binary data = new Binary(b);
            BasicDBObject o = new BasicDBObject();
            o.append("name",empname).append("photo",data);
            collection.insert(o);
            System.out.println("Inserted record.");
 
            f.close();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
   
}