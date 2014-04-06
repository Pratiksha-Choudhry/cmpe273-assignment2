package edu.sjsu.cmpe.procurement.jobs;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;
import edu.sjsu.cmpe.procurement.domain.BookDetails;

/**
 * This job will run at every 5 second.
 */
@Every("5s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void doJob() {
//	String strResponse = ProcurementService.jerseyClient.resource(
//		"http://ip.jsontest.com/").get(String.class);
//	log.debug("Response from jsontest.com: {}", strResponse);
    	 try{
    			postToPublisher(ProcurementService.queueName, ProcurementService.topicName);
    			}catch(Exception e)
    				{System.out.println(e.getMessage());}
    			
    			try{
    				getFromPublisher();
    				}catch(Exception e)
    					{ System.out.println(e.getMessage()); }
    
}

  //HTTP POST(Post to publisher)-Point to point connection
public void postToPublisher(String qname, String tname)throws Exception
{
	String queueName = qname;
	String user = "admin";
	String password = "password";
	String host = "54.193.56.218";
	int port = Integer.parseInt("61613");
	String queue = queueName;
	
	String destination = queueName;

	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + host + ":" + port);

	Connection connection = factory.createConnection(user, password);
	connection.start();
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination dest = new StompJmsDestination(destination);

	MessageConsumer consumer = session.createConsumer(dest);
	System.out.println("Waiting for messages from " + queue + "...");
	String start = "{\"id\":\"07974\",\"order_book_isbns\":[";
	String end = "]}";
	String middle ="";
	long waittime = 5000; 
	
	
	while(true) {
		Message msg = consumer.receive(waittime);
	    if( msg instanceof  TextMessage ) 
	    {
	    String body = ((TextMessage) msg).getText();
	    middle = middle+body.substring(body.indexOf(":")+1);
	    middle = middle+",";    	      	    
	    System.out.println("Getting messages from "+queue+" :Message:"+body);
   	    }
	    else if(msg==null){
	    	System.out.println("Exiting: Queue is empty: exiting after:"+waittime/1000 +":seconds");
	    	break;
	    }
	    else{
	    	System.out.println("Unexpected message type: "+msg.getClass());
	    }
	}
	connection.close();
	String input =null;
	System.out.println("Middle:"+middle);
	if(middle == null || middle.isEmpty()) {
		System.out.println("Nothing received");
	} else {
    	input = start+middle.substring(0,middle.lastIndexOf(','))+end; 
    	System.out.println("Input:"+input);
    	final Client client = Client.create();
    	WebResource webResource = client.resource("http://54.193.56.218:9000/orders");
    	ClientResponse response = webResource.type("application/json").post(ClientResponse.class, input);
    	System.out.println("response:"+response.toString());
    	String output = response.getEntity(String.class);
    	System.out.println("response:"+output);         	
	}
	
	   	
}
public void getFromPublisher()throws JMSException
{
	
	/*HTTP GET Connection*/
	Client client_resp = Client.create();
	 
	WebResource webRes = client_resp
	   .resource("http://54.193.56.218:9000/orders/07974");

	ClientResponse resp = webRes.accept("application/json").get(ClientResponse.class);

	if (resp.getStatus() != 200) {
	   throw new RuntimeException("Failed : HTTP error code : "
		+ resp.getStatus());
	}
    
	BookDetails book=resp.getEntity(BookDetails.class);
	
	/* Publish to different topics based on category*/
	String user = "admin";
	String password =  "password";
	String host =  "54.193.56.218";
	int port = Integer.parseInt("61613");
	
	//Create connection and start session
	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + host + ":" + port);
	Connection connection = factory.createConnection(user, password);
	connection.start();    	
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	System.out.println("Connection started");
	
	//For each topic, publish to different destinations
	for(int i=0;i<book.getShipped_books().length;i++)
	{
		Destination destination = new StompJmsDestination("/topic/07974.book."+book.getShipped_books()[i].getCategory());
		MessageProducer producer = session.createProducer(destination);	
		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		String data = book.getShipped_books()[i].getIsbn()+":"+"\""+book.getShipped_books()[i].getTitle()+"\":\""+book.getShipped_books()[i].getCategory()+"\":\""+book.getShipped_books()[i].getCoverimage()+"\"";
		System.out.println("data:"+data);
		TextMessage msg = session.createTextMessage(data);
		msg.setLongProperty("id", System.currentTimeMillis());
		producer.send(msg);
		
	}
	connection.close();
}
}