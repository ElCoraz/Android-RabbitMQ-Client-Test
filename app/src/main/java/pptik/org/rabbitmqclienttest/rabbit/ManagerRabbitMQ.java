package pptik.org.rabbitmqclienttest.rabbit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import pptik.org.rabbitmqclienttest.utilities.Message;
/**************************************************************************************************/
public class ManagerRabbitMQ {
    /**********************************************************************************************/
    protected Channel mChannel = null;
    protected Connection mConnection;
    /**********************************************************************************************/
    private static final String EXCHANGE_NAME = "amq.direct";
    private static final String ACTION_STRING_ACTIVITY = "broadcast_event";
    /**********************************************************************************************/
    String userName = "admin";
    String password = "4217777";
    String virtualHost = "/";
    String serverIp = "192.168.0.156";
    int port = 5672;
    /**********************************************************************************************/
    protected boolean running;
    /**********************************************************************************************/
    private final Context context;
    /**********************************************************************************************/
    public ManagerRabbitMQ(Context context) {
        this.context = context;
    }
    /**********************************************************************************************/
    public void dispose() {
        running = false;
        try {
            if (mConnection != null)
                mConnection.close();
            if (mChannel != null)
                mChannel.abort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**********************************************************************************************/
    @SuppressLint("StaticFieldLeak")
    public void connectToRabbitMQ() {
        if (mChannel != null && mChannel.isOpen()) {
            running = true;
        }
        new AsyncTask<Void, Void, Boolean>() {
            /**************************************************************************************/
            @Override
            protected Boolean doInBackground(Void... voids) {
                final ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.setUsername(userName);
                connectionFactory.setPassword(password);
                connectionFactory.setVirtualHost(virtualHost);
                connectionFactory.setHost(serverIp);
                connectionFactory.setPort(port);
                connectionFactory.setAutomaticRecoveryEnabled(true);
                try {
                    mChannel = connectionFactory.newConnection().createChannel();
                    registerChanelHost();
                } catch (IOException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            /**************************************************************************************/
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                running = aBoolean;
            }
        }.execute();
    }
    /**********************************************************************************************/
    private void registerChanelListHost() {
        try {
            mChannel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
            String queueName = mChannel.queueDeclare().getQueue();
            mChannel.queueBind(queueName, EXCHANGE_NAME, "topic1");
            Consumer consumer = new DefaultConsumer(mChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) {
                    getHeader(properties);
                    String message = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        message = new String(body, StandardCharsets.UTF_8);
                    }
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Message>>() {
                    }.getType();
                    List<Message> messageList = gson.fromJson(message, type);
                }
            };

            mChannel.basicConsume(queueName, true, consumer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**********************************************************************************************/
    private void registerChanelHost() {
        try {
            mChannel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
            final String queueName = mChannel.queueDeclare().getQueue();
            mChannel.queueBind(queueName, EXCHANGE_NAME, "topic1");
            Consumer consumer = new DefaultConsumer(mChannel) {
                /**********************************************************************************/
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) {

                    String message = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        message = new String(body, StandardCharsets.UTF_8);
                    }
                    sendBroadcast(message);
                }
            };
            mChannel.basicConsume(queueName, true, consumer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**********************************************************************************************/
    private void sendBroadcast(String msg) {
        Intent intent = new Intent(ACTION_STRING_ACTIVITY);
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    /**********************************************************************************************/
    private void getHeader(AMQP.BasicProperties properties) {
        Map<String, Object> headers = properties.getHeaders();
        Object deviceId = headers.get("extraContent");
    }
    /**********************************************************************************************/
    @SuppressLint("StaticFieldLeak")
    public void sendMessage(String msg) {
        new AsyncTask<Void, Void, Boolean>() {
            /**************************************************************************************/
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    mChannel.basicPublish(EXCHANGE_NAME, "topic1", null, msg.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            /**************************************************************************************/
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                running = aBoolean;
            }
        }.execute();
    }
}
