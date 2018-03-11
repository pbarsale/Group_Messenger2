package edu.buffalo.cse.cse486586.groupmessenger2;

import java.net.SocketException;
import java.util.*;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.content.ContentResolver;
import android.content.ContentValues;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import android.util.Log;
import java.net.ServerSocket;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

/* References :
Content Provider Concepts:
1. https://developer.android.com/guide/topics/providers/content-provider-creating.html

Multicast Ordering and Failure Handling Concepts:
1. https://www.youtube.com/watch?v=qhL7GW1KOj8&list=PLFd87qVsaLhOkTLvfp6MC94iFa_1c9wrU&index=51
2. https://www.youtube.com/watch?v=yHRYetSvyjU&list=PLFd87qVsaLhOkTLvfp6MC94iFa_1c9wrU&index=52
3. https://www.youtube.com/watch?v=lugR1CIIU4w&index=53&list=PLFd87qVsaLhOkTLvfp6MC94iFa_1c9wrU
4. https://www.youtube.com/watch?v=J076S7E33bo&index=55&list=PLFd87qVsaLhOkTLvfp6MC94iFa_1c9wrU

 */


public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static int count = 0;

    static int proposed = -1;
    static int agreed = -1;
    static int msgNo = 0;
    static String failed = "NA";


    /*
    Referred for How to use priority queue comparator
    1. https://javabeat.net/ordering-queue-using-comparator-interface-and-priorityqueue/
     */
    PriorityQueue<HoldBack> holdBackQueue = new PriorityQueue<HoldBack>(30, new Comparator<HoldBack>() {
        @Override
        public int compare(HoldBack lhs, HoldBack rhs) {

            if(lhs.getProposed()> rhs.getProposed())
            {
                return 1;
            }
            else if(lhs.getProposed()< rhs.getProposed())
            {
                return -1;
            }
            else
            {
                if(Integer.parseInt(lhs.sender_port) > Integer.parseInt(rhs.sender_port))
                    return 1;

                else if(Integer.parseInt(lhs.sender_port) < Integer.parseInt(rhs.sender_port))
                    return -1;
                else
                    return 0;
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }

        catch (IOException e) {
            Log.e("On Create", "IOException Catch in the code");
            return;
        }
        catch (Exception e)
        {
            Log.e("On Create", "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(

                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

              //  Log.d("OnClick", "Execution Started");

                String msg = editText.getText().toString();
             //   Log.d("OnClick", "Message  " + msg);

                editText.setText(""); // This is one way to reset the input box

                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.

                // Get the highest new proposed value
                proposed = Math.max(agreed,proposed) + 1;

                //Log.d("OnClick", "proposed  " + proposed);

                msgNo++;

                // Msgid so as to uniquely identify a single message in the system
                final String final_msgId = myPort + "@" + msgNo;

               // Log.d("OnClick", "MessageId  " + final_msgId);

              //  Log.d("OnClick", "Calling Initial Client Task");
                // Set message type to initial
                new ClientTask(msg, final_msgId, proposed,myPort,"").executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Initial");
            }
        });
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri mUri;
        private ContentValues mContentValues;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

           // Log.d("ServerTask", "Starting the code");

            //  Log.d(TAG, "doInBackground: Socket accept done");
            //  dataInputStream = new DataInputStream(socket.getInputStream());
            //   Log.d(TAG, "doInBackground: Starting the code");

            ServerSocket serverSocket = sockets[0];
            Socket socket = null;
            try {
                while(true) {

                  //  Log.d("ServerTask", "Inside while true");

                    //  Log.d(TAG, "doInBackground: In try");
                    // Server will accept the connection from the client
                    socket = serverSocket.accept();
                    // socket.setTcpNoDelay(true);

                    //  Log.d(TAG, "doInBackground: Accepted");

                    // This will read the message sent on the InputStream
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

                    // Read the message line by line
                    String line = in.readLine();

                   // Log.d("ServerTask", "Line read " + line);

                    if (line != null)
                {
                    String lines[] = line.split("###");

                   // Log.d("ServerTask", "Lines length " + lines.length);


                    //  Log.d(TAG, "doInBackground: Read the line");
                    //  Log.d(TAG, "doInBackground: Line " + line);

                    String message = lines[0];
                    String msgId = lines[1];
                    int rec_proposed = Integer.parseInt(lines[2]);
                    String sender_port = lines[3];
                    String rec_port = lines[4];
                    String isreply = lines[5];
                    failed = lines[6];

                    // P1 means first proposal message from the client
                    if (isreply.equals("P1")) {
                       // Log.d("ServerTask", "isreply " + isreply);

                        if (!sender_port.equals(rec_port)) {
                           // Log.d("ServerTask", "This is not sender");

                            // Calculate your own latest new proposal
                            int new_proposed = Math.max(agreed, proposed) + 1;

                            if (new_proposed > rec_proposed)
                                rec_proposed = new_proposed;

                            proposed = rec_proposed;

                            // Keep the message and its details in the HoldBack Queue
                            HoldBack hb = new HoldBack(message, msgId, proposed, sender_port, false);
                            holdBackQueue.add(hb);

                            // Send reply to the client about the new proposal
                            PrintWriter out =
                                    new PrintWriter(socket.getOutputStream(), true);
                            out.println(message + "###" + msgId + "###" + proposed + "###" + rec_port + "###" + sender_port + "###P2###" + failed);
                            out.flush();

                        } else {
                           // Log.d("ServerTask", "This me me");

                            // This runs when it is my own message
                            // Just send back the same proposal
                            PrintWriter out =
                                    new PrintWriter(socket.getOutputStream(), true);
                            out.println(message + "###" + msgId + "###" + rec_proposed + "###" + rec_port + "###" + sender_port + "###P2###" + failed);
                            out.flush();
                        }
                    }

                    // Agree means final agreed number for a message from the client
                    else if (isreply.equals("Agree")) {
                        //Log.d("ServerTask", "isreply " + isreply);


                        if (!sender_port.equals(rec_port)) {
                            //Log.d("ServerTask", "Not me ");

                            // Update your agreed variable
                            agreed = Math.max(rec_proposed,agreed);

                           // Log.d("ServerTask", "Changed Agreed " + agreed);

                            /*
                            Referred for Iterating through priority queue
                            https://www.tutorialspoint.com/java/util/priorityqueue_iterator.htm
                            */

                            Iterator<HoldBack> it = holdBackQueue.iterator();

                            // Iterate through the holdback queue and change the priority of the message
                            // Also set the delivery true
                            while (it.hasNext()) {
                                HoldBack hb_temp = it.next();
                                if (hb_temp.getMsgId().equals(msgId)) {

                                   // Log.d("ServerTask", "Found the message ");

                                    if(hb_temp.getProposed()<=rec_proposed)
                                    {
                                        holdBackQueue.remove(hb_temp);
                                        hb_temp.setProposed(rec_proposed);
                                        hb_temp.setDeliver(true);
                                        holdBackQueue.add(hb_temp);
                                       // Log.d("ServerTask", "Changed priority and delivery to true ");
                                    }
                                    break;
                                }
                            }
                        }

                       // Log.d("ServerTask", "Final Delivery");

                        // Send back reply to the client to confirm the updation
                        PrintWriter out =
                                new PrintWriter(socket.getOutputStream(), true);
                        out.println("Final Delivery");
                        out.flush();
                    }
                }

                // Check the holdback queue periodically and deliver the messages
                    while (holdBackQueue.peek()!=null) {
                     //   Log.d("ServerTask", "Checking the queue");

                        // If message sender is failed, just remove the message from the queue
                        if(holdBackQueue.peek().getSender_port().equals(failed))
                        {
                            holdBackQueue.poll();
                            continue;
                        }

                        if (holdBackQueue.peek().isDeliver() == true) {

                           // Log.d("ServerTask", "There is something to poll");

                            HoldBack hb_temp = holdBackQueue.poll();

                          //  Log.d("ServerTask", "polled the same");

                            if (hb_temp.msg != null) {

                               // Log.d("ServerTask", "Polled message not null");

                                mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                mContentValues = new ContentValues();
                                mContentValues.put("key", String.valueOf(count));
                                count++;
                                mContentValues.put("value", hb_temp.msg);
                                getContentResolver().insert(mUri, mContentValues);
                               // Log.d("ServerTask", "Inserted the value");

                               // Log.d("ServerTask", "Call publish progress for message : " + hb_temp.msgId);
                                publishProgress(new String[]{hb_temp.msg});
                            }
                        }
                        else
                            break;
                    }
                }
            } catch (SocketTimeoutException e) {
                Log.e("Server Task", "Alert Time Out Exception Catch in the code");
            } catch (IOException e) {
                Log.e("Server Task", "Alert IOException Catch in the code");
            }
            catch (Exception e) {
                Log.e("Server Task", "Alert Exception Catch in the code");
            }
            return null;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        String msg,myPort,msgId,remotePort;
        int final_proposed;

        ClientTask(String msg,String msgId,int final_proposed,String myPort, String remotePort)
        {
            this.msg = msg;
            this.msgId = msgId;
            this.final_proposed = final_proposed;
            this.myPort = myPort;
            this.remotePort = remotePort;
        }

        @Override
        protected Void doInBackground(String... msgs) {

               // Log.d("ClientTask", "Starting the code");

                String msgType = msgs[0];
               // Log.d("ClientTask", "Message Type " + msgType);

            // Initial message after clicking on the send button
                if(msgType.equals("Initial"))
                {
                    // Keep the message in the holdback queue
                    HoldBack hb = new HoldBack(msg, msgId, final_proposed, myPort, false);
                    holdBackQueue.add(hb);

                   // Log.d("ClientTask", "Inside Initial");
                    Socket socket[] = new Socket[REMOTE_PORTS.length];

                    // Send the proposed value to all the ports
                    for(int i=0;i<REMOTE_PORTS.length;i++) {

                        try
                        {
                            // Ignore the failed AVDs
                            if(failed.equals(REMOTE_PORTS[i]))
                            {
                               // Log.d("ClientTask", "Failed AVD : " + REMOTE_PORTS[i]);
                                continue;
                            }

                            String remotePort = REMOTE_PORTS[i];

                           // Log.d("ClientTask", "Send proposal for " + msgId + " to " + remotePort);

                            socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            socket[i].setSoTimeout(500);

                            //  Log.d(TAG, "Client: " + msgToSend);

                            // PrintWriter will send the message to the IP binded to the socket
                            PrintWriter out =
                                    new PrintWriter(socket[i].getOutputStream(), true);

                           // Log.d(TAG, msg + "###" + msgId + "###" + final_proposed + "###" + myPort + "###" + remotePort + "###P1###" + failed);

                            // Log.d(TAG, "Client: PrintWriter Created");
                            out.println(msg + "###" + msgId + "###" + final_proposed + "###" + myPort + "###" + remotePort + "###P1###" + failed);
                            out.flush();

                            // get back the proposal from each of the process
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket[i].getInputStream()));

                            String line = in.readLine();

                            if (line != null) {

                                //Log.d("Client Task", "Line read " + line);
                                String lines[] = line.split("###");

                               // Log.d("Client Task", "Lines length " + lines.length);

                                String message = lines[0];
                                String msgId = lines[1];
                                int rec_proposed = Integer.parseInt(lines[2]);
                                String sender_port = lines[3];
                                String rec_port = lines[4];
                                String isreply = lines[5];
                                failed = lines[6];

                               // Log.d("Client task", "isReply " + isreply);

                                // Update the proposal as per the reply from the processes
                                if(final_proposed<rec_proposed)
                                    final_proposed = rec_proposed;
                            }

                            out.close();
                            socket[i].close();
                        }
                        catch (UnknownHostException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];

                            Log.e(TAG, "Pratibha Alert ClientTask UnknownHostException");
                        } catch (SocketTimeoutException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask socket time out");
                        } catch (IOException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask socket IOException");
                        }

                    }

                    // Update the message in the HoldBack queue and set delivery to true
                    Iterator<HoldBack> it = holdBackQueue.iterator();

                    while (it.hasNext()) {
                        HoldBack hb_temp = it.next();
                        if (hb_temp.getMsgId().equals(msgId)) {

                            holdBackQueue.remove(hb_temp);
                            hb_temp.setProposed(final_proposed);
                            hb_temp.setDeliver(true);
                            holdBackQueue.add(hb_temp);
                            break;
                        }
                    }

                    //Log.d("Client Task", "Agreed Priority for " + msgId + " is " + final_proposed);
                    // Update the latest agreed message
                    agreed = final_proposed;
                    Socket socket1[] = new Socket[REMOTE_PORTS.length];

                    // Send back the latest updated agreed value for this message to all the processes
                    for(int i=0;i<REMOTE_PORTS.length;i++) {

                        try
                        {
                            if(failed.equals(REMOTE_PORTS[i]))
                            {
                               // Log.d("Client Task", "This is failed AVD " + REMOTE_PORTS[i]);
                                continue;
                            }

                            String remotePort = REMOTE_PORTS[i];
                           // Log.d("ClientTask", "Sending agreement for " + msgId + " to : " + REMOTE_PORTS[i]);


                            socket1[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            socket1[i].setSoTimeout(500);

                            PrintWriter out =
                                    new PrintWriter(socket1[i].getOutputStream(), true);

                           // Log.d(TAG, msg + "###" + msgId + "###" + final_proposed + "###" + myPort + "###" + remotePort + "###Agree###" + failed);

                            // Log.d(TAG, "Client: PrintWriter Created");
                            out.println(msg + "###" + msgId + "###" + final_proposed + "###" + myPort + "###" + remotePort + "###Agree###" + failed);
                            out.flush();

                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket1[i].getInputStream()));

                            String line = in.readLine();
                            out.close();
                            socket1[i].close();

                        }
                        catch (UnknownHostException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask UnknownHostException");
                        } catch (SocketTimeoutException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask socket time out");
                        } catch (IOException e) {
                            if(!REMOTE_PORTS[i].equals(myPort))
                                failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask socket IOException");
                        } /*catch(InterruptedException e)
                        {
                            failed = REMOTE_PORTS[i];
                            Log.e(TAG, "Pratibha Alert ClientTask Interrupt");
                        }*/
                    }
                }
            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;

    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    protected void onProgressUpdate(String...strings) {
        /*
             * The following code displays what is received in doInBackground().
             */
        String strReceived = strings[0].trim();
        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        remoteTextView.append(strReceived + "\t\n");
    }
}
