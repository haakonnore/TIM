/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.engine;

import gov.nist.javax.sip.header.CallID;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;
import javax.sip.address.URI;
import javax.swing.text.AbstractDocument.Content;

import java.net.*;

import java.text.ParseException;
import java.util.*;

import orca.*;

import orca.tools.Configuration;
import orca.engine.MessengerEngine;
import orca.tools.SdpInfo;
import orca.tools.SdpManager;
import sun.security.pkcs.ContentInfo;

/**
 *
 * @author prajwalan
 */
public class MessengerEngine implements SipListener
{
    private Configuration conf;
    private SipURI myURI;
    private SipFactory mySipFactory;
    private SipStack mySipStack;
    private ListeningPoint myListeningPoint;
    private SipProvider mySipProvider;
    private MessageFactory myMessageFactory;
    private HeaderFactory myHeaderFactory;
    private AddressFactory myAddressFactory;
    private Properties myProperties;
    private OrcaGUI myGUI;
    private ContactHeader myContactHeader;
    private ViaHeader myViaHeader;
    private Address fromAddress;
    private Dialog myDialog;
    private ClientTransaction myClientTransaction;
    private ServerTransaction myServerTransaction;
    public int status;
    private String myIP;
    private SdpManager mySdpManager;
    private SdpInfo answerInfo;
    private SdpInfo offerInfo;

    private int myPort;
    private String myServer;
    private int myAudioPort;
    private int myVideoPort;
    private int myAudioCodec;
    private int myVideoCodec;

    static final int YES=0;
    static final int NO=1;
    static final int SEND_MESSAGE=2;

    static final int UNREGISTERED=-2;
    static final int REGISTERING=-1;

    public static final int IDLE=0;
    public static final int WAIT_PROV=1;
    public static final int WAIT_FINAL=2;
    public static final int ESTABLISHED=4;
    public static final int RINGING=5;
    public static final int WAIT_ACK=6;
    public static final String xml1= "<?xml version='1.0' encoding='UTF-8'?><presence xmlns='urn:ietf:params:xml:ns:pidf' xmlns:dm='urn:ietf:params:xml:ns:pidf:data-model' xmlns:rpid='urn:ietf:params:xml:ns:pidf:rpid' xmlns:c='urn:ietf:params:xml:ns:pidf:cipid' entity=";
 	public static final String xml2= "><tuple id=";
 	public static final String xml3= "><status><basic>open</basic></status></tuple><dm:person id=";
	public static final String xml4= "><rpid:activities><rpid:";
	public static final String xml5= "/></rpid:activities><dm:note>";
	public static final String xml6 = "</dm:note></dm:person></presence>";
    
    private long SUBSCRIBE_COUNT = 1L;
    private long PUBLISH_COUNT = 1L;
    
    private String tupleID = "";
    private String personID = "";
    
    
    
    private Timer subscribeTimerTask=null;
    private Timer locationTimerTask=null;
    private boolean locationTimerTaskRunning = false;

    class MyTimerTask extends TimerTask 
    {
        MessengerEngine myListener;

        public MyTimerTask (MessengerEngine myListener)
        {
            this.myListener=myListener;
        }

        public void run() 
        {
            try
            {
                Request myBye = myListener.myDialog.createRequest("BYE");
                myBye.addHeader(myListener.myContactHeader);
                myListener.myClientTransaction =
                myListener.mySipProvider.getNewClientTransaction(myBye);
                myListener.myDialog.sendRequest(myListener.myClientTransaction);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    class SubscribeTimerTask extends TimerTask 
    {
        MessengerEngine engine;

        public SubscribeTimerTask (MessengerEngine engine)
        {
            this.engine=engine;
        }

        public void run() 
        {
            try
            {
                engine.myGUI.sendSubscribe(null);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    
    public MessengerEngine(Configuration conf, OrcaGUI GUI, String sipserver)  
            throws Exception
    {
        this.conf = conf;

        myServer=sipserver;
        myGUI = GUI;
        //myIP = InetAddress.getLocalHost().getHostAddress();
        Socket s = new Socket("java.com", 80);
        myIP = s.getLocalAddress().getHostAddress(); // this what actually works
        s.close();
        myPort = conf.sipPort;

        mySdpManager=new SdpManager();

        answerInfo=new SdpInfo();
        offerInfo=new SdpInfo();

        tupleID = conf.userID;
        personID = conf.userID;

        mySipFactory = SipFactory.getInstance();
        mySipFactory.setPathName("gov.nist");
        myProperties = new Properties();
        myProperties.setProperty("javax.sip.STACK_NAME", "myStack");
        mySipStack = mySipFactory.createSipStack(myProperties);
        myMessageFactory = mySipFactory.createMessageFactory();
        myHeaderFactory = mySipFactory.createHeaderFactory();
        myAddressFactory = mySipFactory.createAddressFactory();
        myListeningPoint = mySipStack.createListeningPoint(myIP, myPort, "udp");
        mySipProvider = mySipStack.createSipProvider(myListeningPoint);
        mySipProvider.addSipListener(this);

        myURI = myAddressFactory.createSipURI(
                myGUI.messengerConfiguration.id, 
                myGUI.messengerConfiguration.sipProxyIP);
        myURI.setPort(myGUI.messengerConfiguration.sipProxyPort);

        SipURI contactURI = myAddressFactory.createSipURI(myURI.getUser(), myIP);
        Address contactAddress = myAddressFactory.createAddress(contactURI);
        myContactHeader = myHeaderFactory.createContactHeader(contactAddress);
        contactURI.setPort(myPort);

        myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

        SipURI fromURI = myAddressFactory.createSipURI(
                myGUI.messengerConfiguration.id,
                myGUI.messengerConfiguration.sipProxyIP
                );
        fromAddress=myAddressFactory.createAddress(fromURI);
        fromURI.setPort(myGUI.messengerConfiguration.sipProxyPort);
        myGUI.setStatusTextArea("Initialized at IP "+ myIP+", port "+myPort);


        Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
        Address registerToAddress = fromAddress;
        Address registerFromAddress=fromAddress;

        ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
        FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

        //ArrayList myViaHeaders = new ArrayList();
        ArrayList<ViaHeader> myViaHeaders = new ArrayList<ViaHeader>();
        myViaHeaders.add(myViaHeader);

        MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
        CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(1L,"REGISTER");
        ExpiresHeader myExpiresHeader=myHeaderFactory.createExpiresHeader(60000);
        CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
        javax.sip.address.URI myRequestURI = registrarAddress.getURI();
        //SipURI myRequestURI = (SipURI) registrarAddress.getURI();
        Request myRegisterRequest =
                myMessageFactory.createRequest(
                myRequestURI,
                "REGISTER",
                myCallIDHeader, 
                myCSeqHeader, 
                myFromHeader, 
                myToHeader,
                myViaHeaders, 
                myMaxForwardsHeader);
        myRegisterRequest.addHeader(myContactHeader);
        myRegisterRequest.addHeader(myExpiresHeader);

        myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
        myClientTransaction.sendRequest();

        myGUI.log("[SENT] " + myRegisterRequest.toString());
        setStatus(REGISTERING);
    }

    
    
    public void setOff()
    {
        try
        {
            killSubscribeTimerTask();
            //killLocationTimerTask();
            mySipProvider.removeSipListener(this);
            mySipProvider.removeListeningPoint(myListeningPoint);
            mySipStack.deleteListeningPoint(myListeningPoint);
            mySipStack.deleteSipProvider(mySipProvider);
            myListeningPoint=null;
            mySipProvider=null;
            mySipStack=null;

            myGUI.showStatus("");
        }
        catch(Exception e){}
    }


    public void updateConfiguration(Configuration conf) 
    {
        myPort = conf.sipPort;
    }

    public void processRequest(RequestEvent requestReceivedEvent) 
    {
        Request myRequest=requestReceivedEvent.getRequest();
        String method=myRequest.getMethod();
        myGUI.log("[RECEIVED] "+myRequest.toString());
        if (!method.equals("CANCEL")) 
        {
            myServerTransaction=requestReceivedEvent.getServerTransaction();
        }

        try
        {
            switch (status) 
            {
            case IDLE:
                if (method.equals("INVITE")) 
                {
                    processInviteRequest(myRequest);
                }
                else if (method.equals("MESSAGE")) 
                {
                    processMessageRequestWhenIdle(myRequest);
                }
                else if (method.equals("NOTIFY"))
                {
                	System.out.println("Recieved Notify (in pocessRequest()) ");
                    handleNotify(myRequest);
                }
                
            break;

            case ESTABLISHED:
                if (method.equals("BYE")) 
                {
                    processByeRequest(myRequest);
                }
                else if (method.equals("MESSAGE")) 
                {
                    processMessageRequestWhenEstablished(myRequest);
                }
                else if (method.equals("NOTIFY"))
                {
                    handleNotify(myRequest);
                }
            break;

            case RINGING:
                if (method.equals("CANCEL")) 
                {
                    processCancelRequest(requestReceivedEvent, myRequest);
                }
            break;

            case WAIT_ACK:
                if (method.equals("ACK")) 
                {
                    setStatus(ESTABLISHED);
                }
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) 
    {
        try
        {
            Response myResponse=responseReceivedEvent.getResponse();
            myGUI.log("[RECEIVED] "+myResponse.toString());
            ClientTransaction thisClientTransaction=responseReceivedEvent.getClientTransaction();
            if (!thisClientTransaction.equals(myClientTransaction)) 
            {
                return;
            }
            int myStatusCode=myResponse.getStatusCode();
            CSeqHeader originalCSeq=
                    (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
            long numseq=originalCSeq.getSeqNumber();

            switch(status)
            {
            case WAIT_PROV:
                if (myStatusCode<200) //Provisional (1xx)
                {
                    processProvisionalResponse(thisClientTransaction);
                }
                else if (myStatusCode<300) //Successful (2xx)
                {
                    processSuccessfulResponse(thisClientTransaction, myResponse, numseq);
                }
                else //Redirection (3xx), Request Failure (4xx), Server Failure (5xx) Global Failure (6xx)
                {
                    processUnSuccessfulResponse(numseq);
                }
            break;

            case WAIT_FINAL:
                if (myStatusCode<200) //Provisional (1xx)
                {
                    processProvisionalResponse(thisClientTransaction);
                }
                else if (myStatusCode<300) //Successful (2xx)
                {
                    processSuccessfulResponse(thisClientTransaction, myResponse, numseq);

                }
                else //Redirection (3xx), Request Failure (4xx), Server Failure (5xx) Global Failure (6xx)
                {
                    processUnSuccessfulFinalResponse(myStatusCode);
                }
            break;

            case REGISTERING:
                if (myStatusCode==200) //Successful
                {
                    processSuccessfulRegisteringResponse();
                }
                else 
                {
                    setStatus(UNREGISTERED);
                }
            break;
            }
        }
        catch(Exception excep)
        {
            excep.printStackTrace();
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) 
    {
    }

    public void processTransactionTerminated(TransactionTerminatedEvent tevent) 
    {
    }

    public void processDialogTerminated(DialogTerminatedEvent tevent) 
    {
    }

    public void processIOException(IOExceptionEvent tevent) 
    {
    }

    public void processInviteRequest(Request myRequest)
            throws TransactionAlreadyExistsException,
                    TransactionUnavailableException, ParseException,
                    SipException, InvalidArgumentException
    {
        if (myServerTransaction == null) 
        {
            myServerTransaction = 
                    mySipProvider.getNewServerTransaction(myRequest);
        }

        byte[] cont=(byte[]) myRequest.getContent();
        offerInfo=mySdpManager.getSdp(cont);

        answerInfo.IpAddress=myIP;
        answerInfo.aport=myAudioPort;
        answerInfo.aformat=offerInfo.aformat;

        if (offerInfo.vport==-1) 
        {
            answerInfo.vport=-1;
        }
        else if (myVideoPort==-1) 
        {
            answerInfo.vport=0;
            answerInfo.vformat=offerInfo.vformat;
        }
        else 
        {
            answerInfo.vport=myVideoPort;
            answerInfo.vformat=offerInfo.vformat;
        }

        Response myResponse=myMessageFactory.createResponse(180,myRequest);
        myResponse.addHeader(myContactHeader);
        ToHeader myToHeader = (ToHeader) myResponse.getHeader("To");
        myToHeader.setTag("454326");
        myServerTransaction.sendResponse(myResponse);
        myDialog=myServerTransaction.getDialog();
        myGUI.log("[SENT] "+myResponse.toString());
        setStatus(RINGING);        
    }
    
    public void processMessageRequestWhenIdle(Request myRequest)
                throws TransactionAlreadyExistsException,
                    TransactionUnavailableException, ParseException,
                    SipException, InvalidArgumentException            
    {
        if (myServerTransaction == null) 
        {
            myServerTransaction = mySipProvider.getNewServerTransaction(myRequest);
        }
        Response myResponse=myMessageFactory.createResponse(200,myRequest);
        myResponse.addHeader(myContactHeader);
        ToHeader myToHeader = (ToHeader) myResponse.getHeader("To");


        //FromHeader myFromHeader = (FromHeader) myRequest.getHeader("From");
        //javax.sip.address.Address messageFromAddress=myFromHeader.getAddress();

        byte[] myByteContent=myRequest.getRawContent();
        String myContent=new String(myByteContent);

        myToHeader.setTag("454326");
        myServerTransaction.sendResponse(myResponse);

        myGUI.displayMessage(myContent);
        myGUI.log("[SENT] "+myResponse.toString());
    }
    
    public void processByeRequest(Request myRequest)
            throws ParseException,
                    SipException, InvalidArgumentException 
    {
        Response myResponse=myMessageFactory.createResponse(200,myRequest);
        myResponse.addHeader(myContactHeader);
        myServerTransaction.sendResponse(myResponse);
        myGUI.log("[SENT] "+myResponse.toString());

        setStatus(IDLE);        
    }
    
    public void processMessageRequestWhenEstablished(Request myRequest)
            throws ParseException, SipException, InvalidArgumentException
    {
            Response myResponse=myMessageFactory.createResponse(200,myRequest);
            myResponse.addHeader(myContactHeader);

            //FromHeader myFromHeader = (FromHeader) myRequest.getHeader("From");
            //javax.sip.address.Address messageFromAddress=myFromHeader.getAddress();

            byte[] myByteContent=myRequest.getRawContent();
            String myContent=new String(myByteContent);

            myServerTransaction.sendResponse(myResponse);
            myGUI.displayMessage(myContent);
            myGUI.log("[SENT] "+myResponse.toString());        
    }
    
    public void processCancelRequest(RequestEvent requestReceivedEvent, Request myRequest)
            throws ParseException, SipException, InvalidArgumentException
    {
        ServerTransaction myCancelServerTransaction=requestReceivedEvent.getServerTransaction();
        Request originalRequest=myServerTransaction.getRequest();
        Response myResponse=myMessageFactory.createResponse(487,originalRequest);
        myServerTransaction.sendResponse(myResponse);
        Response myCancelResponse=myMessageFactory.createResponse(200,myRequest);
        myCancelServerTransaction.sendResponse(myCancelResponse);

        myGUI.log("[SENT] "+myResponse.toString());
        myGUI.log("[SENT] "+myCancelResponse.toString());

        setStatus(IDLE);        
    }
    
    public void processProvisionalResponse(ClientTransaction thisClientTransaction)
    {
        setStatus(WAIT_FINAL);
        myDialog = thisClientTransaction.getDialog();
    }
    
    public void processSuccessfulResponse(ClientTransaction thisClientTransaction,
            Response myResponse, long numseq)
            throws InvalidArgumentException, SipException
    {
        setStatus(ESTABLISHED);
        
        myDialog = thisClientTransaction.getDialog();
        Request myAck = myDialog.createAck(numseq);
        myAck.addHeader(myContactHeader);
        myDialog.sendAck(myAck);
        myGUI.log("[SENT] "+myAck.toString());
    }
    
    public void processUnSuccessfulResponse(long numseq)
            throws InvalidArgumentException, SipException
    {
        setStatus(IDLE);
        Request myAck = myDialog.createAck(numseq);
        myAck.addHeader(myContactHeader);
        myDialog.sendAck(myAck);
        myGUI.log("[SENT] "+myAck.toString());

        myGUI.showErrorMessage("Call to " + ((ToHeader)myAck.getHeader("To")).getAddress() + " Failed");
        myGUI.setStatusTextArea("Call to " + ((ToHeader)myAck.getHeader("To")).getAddress() + " Failed");
        
    }
    
    public void processUnSuccessfulFinalResponse(int myStatusCode)
    {
        if( myStatusCode == 600 || myStatusCode == 486)
        {
            myGUI.setStatusTextArea("Call Rejected or Destination too busy");
            myGUI.showCallRejectedMessage();
        }

        setStatus(IDLE);
        
    }
    
    public void processSuccessfulRegisteringResponse()
    {
        setStatus(IDLE);

        killSubscribeTimerTask();
        subscribeTimerTask = new Timer();
        subscribeTimerTask.schedule(new SubscribeTimerTask(this),
                (conf.subscribeExpire-10)*1000, 
                (conf.subscribeExpire-10)*1000);

        //updateLocationTimer();

        myGUI.setStatusTextArea("Signed in as \n" + conf.userID + 
                "@" + myGUI.messengerConfiguration.sipProxyIP
                + ":" + myGUI.messengerConfiguration.sipProxyPort);
        
    }
        
    public void userInput(int type, String destination, String message)
    {
        try 
        {
            switch (status) 
            {
            case IDLE:
                if (type == YES) 
                {
                    dial(destination);
                }
                else if (type==SEND_MESSAGE) 
                {
                    sendMessageWhenIdle(destination, message);
                }
            break;

            case WAIT_ACK:
                if (type == NO) 
                {
                    sendCancel();
                }
                break;
            case WAIT_PROV:
                if (type == NO) 
                {
                    sendCancel();
                    break;
                }
            case WAIT_FINAL:
                if (type == NO) 
                {
                    sendCancel();
                    break;
                }
            case ESTABLISHED:
                if (type == NO) 
                {
                    sendBye();
                }
                else if (type==SEND_MESSAGE) 
                {
                    sendMessageWhenEstablished(destination, message);
                }
                break;
            case RINGING:
                if (type == NO) 
                {
                    sendBusy();
                    break;
                }
                else if (type == YES) 
                {
                    acceptCall();
                    break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void dial(String destination)
    {
        try
        {
            System.out.println("destination = " + destination);
            Address toAddress = myAddressFactory.createAddress(destination);
            ToHeader myToHeader = myHeaderFactory.createToHeader(toAddress, null);

            FromHeader myFromHeader = myHeaderFactory.createFromHeader(
            fromAddress, "56438");

            myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);
            //ArrayList myViaHeaders = new ArrayList();
            ArrayList<ViaHeader> myViaHeaders = new ArrayList<ViaHeader>();
            myViaHeaders.add(myViaHeader);
            MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.
            createMaxForwardsHeader(70);
            CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(1L,
            "INVITE");
            CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
            javax.sip.address.URI myRequestURI = toAddress.getURI();
            Request myRequest = myMessageFactory.createRequest(myRequestURI,
            "INVITE",
            myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,
            myViaHeaders, myMaxForwardsHeader);

            //myRequest.addFirst(myRouteHeader);
            myRequest.addHeader(myContactHeader);

            //HERE GOES SDP AND MEDIA TOOL (SEND OFFER)

            offerInfo=new SdpInfo();
            offerInfo.IpAddress=myIP;
            offerInfo.aport=myAudioPort;
            offerInfo.aformat=myAudioCodec;
            offerInfo.vport=myVideoPort;
            offerInfo.vformat=myVideoCodec;


            ContentTypeHeader contentTypeHeader=
                    myHeaderFactory.createContentTypeHeader("application","sdp");
            byte[] content=mySdpManager.createSdp(offerInfo);
            myRequest.setContent(content,contentTypeHeader);

            //****************************************************

            myClientTransaction = mySipProvider.getNewClientTransaction(myRequest);
            //String bid=myClientTransaction.getBranchId();

            myClientTransaction.sendRequest();
            myDialog = myClientTransaction.getDialog();
            myGUI.log("[SENT] " + myRequest.toString());

            setStatus(WAIT_PROV);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void sendCancel()
    {
        try
        {
            Request myCancelRequest = myClientTransaction.createCancel();
            ClientTransaction myCancelClientTransaction = mySipProvider.
            getNewClientTransaction(myCancelRequest);
            myCancelClientTransaction.sendRequest();
            myGUI.log("[SENT] " + myCancelRequest.toString());

            setStatus(IDLE);
        }
        catch(Exception e)
        {
            
        }
    }
        
    public void sendBye()
    {
        try
        {
            Request myBye = myDialog.createRequest("BYE");
            myBye.addHeader(myContactHeader);
            myClientTransaction= 
                    mySipProvider.getNewClientTransaction(myBye);
            myDialog.sendRequest(myClientTransaction);
            myGUI.log("[SENT] " + myBye.toString());

            setStatus(IDLE);
        }
        catch(Exception e)
        {
            
        }
        
    }
    
    public void sendMessageWhenEstablished(String destination, String message)
    {
        try
        {
            Request myMessage = myDialog.createRequest("MESSAGE");

            myMessage.addHeader(myContactHeader);
            ContentTypeHeader myContentTypeHeader=
                    myHeaderFactory.createContentTypeHeader("text","plain");

            message = destination + ": " + message;
            byte[] contents = message.getBytes();
            myMessage.setContent(contents,myContentTypeHeader);

            myClientTransaction= 
                    mySipProvider.getNewClientTransaction(myMessage);
            myDialog.sendRequest(myClientTransaction);

            myGUI.displayMessage(message);
            myGUI.log("[SENT] " + myMessage.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void sendMessageWhenIdle(String destination, String message)
    {
        try
        {
            Address toAddress = myAddressFactory.createAddress(destination);
            ToHeader myToHeader = myHeaderFactory.createToHeader(toAddress, null);

            FromHeader myFromHeader = myHeaderFactory.createFromHeader(
            fromAddress, "685354");

            ViaHeader myViaHeader = 
                    myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);
            //ArrayList myViaHeaders = new ArrayList();
            ArrayList<ViaHeader> myViaHeaders = new ArrayList<ViaHeader>();
            myViaHeaders.add(myViaHeader);
            MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.
            createMaxForwardsHeader(70);
            CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(1L,
            "MESSAGE");
            CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
            javax.sip.address.URI myRequestURI = toAddress.getURI();
            Request myRequest = myMessageFactory.createRequest(myRequestURI,
            "MESSAGE",
            myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,
            myViaHeaders, myMaxForwardsHeader);
            //myRequest.addFirst(myRouteHeader);
            myRequest.addHeader(myContactHeader);

            ContentTypeHeader myContentTypeHeader=
                    myHeaderFactory.createContentTypeHeader("text","plain");

            byte[] contents = message.getBytes();
            myRequest.setContent(contents,myContentTypeHeader);

            myClientTransaction = 
                    mySipProvider.getNewClientTransaction(myRequest);
            //String bid=myClientTransaction.getBranchId();

            myClientTransaction.sendRequest();

            String name=fromAddress.getDisplayName();

            myGUI.displayMessage(name+ ":  "+message);

            myGUI.log("[SENT] " + myRequest.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void sendBusy()
    {
        try
        {
            Request originalRequest = myServerTransaction.getRequest();
            //600 -> Busy EVERYWHERE
            //486 -> Busy Here
            Response myResponse = myMessageFactory.createResponse(600,
            originalRequest);
            myServerTransaction.sendResponse(myResponse);
            myGUI.log("[SENT] " + myResponse.toString());

            setStatus(IDLE);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void acceptCall()
    {
        try
        {
            Request originalRequest = myServerTransaction.getRequest();
            Response myResponse = myMessageFactory.createResponse(200,
            originalRequest);
            ToHeader myToHeader = (ToHeader) myResponse.getHeader("To");
            myToHeader.setTag("454326");
            myResponse.addHeader(myContactHeader);

            //SEND ANSWER SDP

            ContentTypeHeader contentTypeHeader=
                    myHeaderFactory.createContentTypeHeader("application","sdp");
            byte[] content=mySdpManager.createSdp(answerInfo);
            myResponse.setContent(content,contentTypeHeader);

            myServerTransaction.sendResponse(myResponse);
            myDialog = myServerTransaction.getDialog();

            new Timer().schedule(new MyTimerTask(this),500000);
            myGUI.log("[SENT] " + myResponse.toString());
            setStatus(WAIT_ACK);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void killSubscribeTimerTask()
    {
        if( subscribeTimerTask != null )
        {
            subscribeTimerTask.cancel();
            subscribeTimerTask = null;
        }
        
    }
   
    public void sendSubscribe(String subscriberAddress)
    {
    	try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	try { 
    		String []a = subscriberAddress.split("@");
    		URI requestURI = myAddressFactory.createSipURI(a[0], a[1]);  
    		CallIdHeader myCallIdHeader =  mySipProvider.getNewCallId();//
    		CSeqHeader myCseqHeader = myHeaderFactory.createCSeqHeader(SUBSCRIBE_COUNT, "SUBSCRIBE");//
    		FromHeader myFromHeader = myHeaderFactory.createFromHeader(fromAddress, "897355");
    		ToHeader myToHeader =myHeaderFactory.createToHeader(myAddressFactory.createAddress("sip:"+subscriberAddress), null);
    		ArrayList<ViaHeader> myViaHeaders = new ArrayList<ViaHeader>(); //
    		myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort, "udp", "z9sG4bKnashds7");//
    		myViaHeaders.add(myViaHeader);
    		MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);//
    		ContactHeader myContactHeader= myHeaderFactory.createContactHeader(fromAddress);
    		ExpiresHeader myExpiresHeader = myHeaderFactory.createExpiresHeader(300);
    		EventHeader myEventHeader = myHeaderFactory.createEventHeader("Presence");
    		Request myRequest = myMessageFactory.createRequest(requestURI,"SUBSCRIBE", myCallIdHeader, myCseqHeader, myFromHeader, myToHeader, myViaHeaders, myMaxForwardsHeader);
    		myRequest.addHeader(myContactHeader);
    		myRequest.addHeader(myExpiresHeader);
    		myRequest.addHeader(myEventHeader);
    		ClientTransaction myClientTransaction= mySipProvider.getNewClientTransaction(myRequest);
    		myClientTransaction.sendRequest();
    		myGUI.log("[SENT] " + myRequest.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
    	// Fill up this method ...
    	/*
    	 * subscriberAddress is a string with the format bob@11.111.11.111:5060, So you need to 
    	 * make SipURI out of it.
    	 * 
    	 * Use myHeaderFactory to create any necessary headers 
    	 * Use myMessageFactory to create any Request messages
    	 * Finally create a new clienttransaction and send the request.
    	 */  	
    }    
    
    public void sendPublish(String statusid, String status) 
    {
        // Fill up this method ...
    	/*
    	 * Use myHeaderFactory to create any necessary headers 
    	 * Use myMessageFactory to create any Request messages
    	 * You must call appropriate method to set the XML content in the Message
    	 * Make sure you also set the content type.
    	 * Finally create a new clienttransaction and send the request.
    	 */
    	try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	try { 
    		String []userData = myURI.toString().split(":");
    		String []userName = userData[1].split("@");
    		
    		
    		URI requestURI = myURI;
    		CallIdHeader myCallIdHeader =  mySipProvider.getNewCallId();//
    		CSeqHeader myCseqHeader = myHeaderFactory.createCSeqHeader(PUBLISH_COUNT, "PUBLISH");//
    		FromHeader myFromHeader = myHeaderFactory.createFromHeader(fromAddress, "564388");//
    		ToHeader myToHeader =myHeaderFactory.createToHeader(myAddressFactory.createAddress(fromAddress.toString()+":" + myPort), null);//
    		ArrayList<ViaHeader> myViaHeaders = new ArrayList<ViaHeader>();// 
    		myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort, "udp", "z9hG4bKnashds7");//
    		myViaHeaders.add(myViaHeader);//
    		MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
    		Address myContactAddress = myAddressFactory.createAddress("sip:"+userName[0]+ "@"+myIP+ ":" + myPort);//
    		ContactHeader myContactHeader= myHeaderFactory.createContactHeader(myContactAddress);//
    		ExpiresHeader myExpiresHeader = myHeaderFactory.createExpiresHeader(300);
    		AllowHeader myAllowHeader = myHeaderFactory.createAllowHeader("INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, SUBSCRIBE, INFO");
    		EventHeader myEventHeader = myHeaderFactory.createEventHeader("Presence");
    		ContentTypeHeader myContentTypeheader = myHeaderFactory.createContentTypeHeader("application", "pidf+xml");
    		String myContent = xml1+"'" + userData[0] + ":" + userData[1] + "'" + xml2 + "'" + userName[0] + "'" + xml3+ "'" + userName[0] + "'"+ xml4+ statusid + xml5 + status + xml6;
    		Request myRequest = myMessageFactory.createRequest(requestURI,"PUBLISH", myCallIdHeader, myCseqHeader, myFromHeader, myToHeader, myViaHeaders, myMaxForwardsHeader);
    		myRequest.addHeader(myContactHeader);//
    		myRequest.addHeader(myExpiresHeader);
    		myRequest.addHeader(myAllowHeader);
    		myRequest.addHeader(myEventHeader);
    		myRequest.addHeader(myContentTypeheader);
    		myRequest.setContent(myContent, myContentTypeheader);
    		myClientTransaction = mySipProvider.getNewClientTransaction(myRequest);
    		myClientTransaction.sendRequest();
    		myGUI.log("[SENT] " + myRequest.toString());
    		
    		
		} catch (Exception e) {
			e.printStackTrace();
		}   
    }    

    private void handleNotify(Request myRequest)
    {
    	System.out.println("Handle Notify is Called");
    	try {    		
    		String pidfContent = new String (myRequest.getRawContent());
    		
    		int indexOfId = pidfContent.lastIndexOf("<person id=") +2;
    		int endId = pidfContent.indexOf("<rpid:activities>")- 3;
    		int indexOfStatus = pidfContent.lastIndexOf("<rpid:activities><rpid:")+ 1;    		
    		int endStatus = pidfContent.indexOf("/></rpid:activities>")- 1;
    		char[] id = null;
    		char[] status = null;
    		
    		pidfContent.getChars(indexOfId, endId, id, 0);
    		pidfContent.getChars(indexOfStatus, endStatus, status, 0);
    		String idToUpdate = "";
    		String statusToUpdate= "";
    		
    		for (int i = 0; i < id.length; i++) {
				idToUpdate += id[i];
			}
    		for (int i = 0; i < status.length; i++) {
				statusToUpdate += id[i];
			}
    		// What to do
    		System.out.println(idToUpdate);
    		System.out.println(statusToUpdate);
    		Response myResponse = myMessageFactory.createResponse(200, myRequest);
			myServerTransaction.sendResponse(myResponse);
			myGUI.log("[SENT] " + myResponse.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

    	
    	// Fill up this method ...
    	/*
    	 * Use myMessageFactory for creating Request or Response messages
    	 * Use myServerTransaction to send Responses
    	 * 
    	 * Call following method to update the GUI's buddy list with new presence status
    	 * myGUI.updateListWithStatus(<entity>, <presencestatus>);
    	 * 
    	 */

    }
    
    public void setStatus(int status)
    {
        this.status = status;
        
        switch (this.status)
        {
            case UNREGISTERED:
                myGUI.showStatus("Status: UNREGISTERED");
                break;
            case REGISTERING:
                myGUI.showStatus("Status: REGISTERING");
                break;
            case IDLE:
                myGUI.showStatus("Status: IDLE");
                myGUI.toggleChatView(false);
                myGUI.enableDisableWhileCalling(true);                
                break;
            case WAIT_PROV:
                myGUI.showStatus("Status: TRYING TO CONNECT");
                break;
            case WAIT_FINAL:
                myGUI.showStatus("Status: ALERTING");
                break;
            case ESTABLISHED:
                myGUI.showStatus("Status: ESTABLISHED");
                myGUI.toggleChatView(true);                
                break;
            case RINGING:
                myGUI.showStatus("Status: RINGING");
                myGUI.enableDisableWhileRinging();
                break;
            case WAIT_ACK:
                myGUI.showStatus("Status: WAITING ACK");
                break;
        }
        
        myGUI.setDialCaption();
    }
    
    public void testLocation()
    {
    	//Write your task 2 code here
    	
    }
    
}
