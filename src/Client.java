import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * This class is the client class for process mqtt request.
 * @author Yanlong LI, u5890571
 * */
public class Client {
    private ArrayList<String> $MessageStream = new ArrayList<>();
    private HashSet<String> $DuplicateMessage = new HashSet<>();
    private String USER_NAME = "students";
    private String PASSWORD = "33106331";
    private String clientID = "3310-u5890571";
    private String HOST = "tcp://comp3310.ddns.net:1883";
    private String TOPIC = "$SYS";
    private int qos = 0;
    private Pattern isDig = Pattern.compile("[0-9]*");

    private MqttClient client;
    private MqttConnectOptions OPTION;

    public Client(){/* Keep default*/}

    public Client(String UserName, String Password, String ClientID, String host, String topic, int Qos){
        this.USER_NAME = UserName;
        this.PASSWORD = Password;
        this.clientID = ClientID;
        this.HOST = host;
        this.TOPIC = topic;
        this.qos = Qos;
    }

    public Client(String topic){
        this.TOPIC = topic;
    }

    public Client(String topic, int Qos){
        this.TOPIC = topic;
        this.qos = Qos;
    }

    protected void start(){
        try{
            client = new MqttClient(HOST, clientID, new MemoryPersistence());
            OPTION = new MqttConnectOptions();
            OPTION.setUserName(USER_NAME);
            OPTION.setPassword(PASSWORD.toCharArray());
            OPTION.setCleanSession(true);
            OPTION.setConnectionTimeout(10);
            OPTION.setKeepAliveInterval(20);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("Connection lost");
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) {
//                    System.out.println("Topic get: " + s);
//                    System.out.println("Qos get: "+ mqttMessage.getQos());
//                    System.out.println("Message get: "+ new String(mqttMessage.getPayload()));
                    // Handel the message only with numbers
                    if(isDig.matcher(new String(mqttMessage.getPayload())).matches()){
                        $MessageStream.add(new String(mqttMessage.getPayload()));
                        $DuplicateMessage.add(new String(mqttMessage.getPayload()));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    System.out.println("Complete-------"+iMqttDeliveryToken.isComplete());

                }
            });
            //可露希尔赛高！CLOSURE IS THE BEST!
            MqttTopic topic = client.getTopic(TOPIC);
            OPTION.setWill(topic, "closed".getBytes(), 1, true);// remove the will message.
            client.connect(OPTION);
            int[] Qos = {qos};
            String[] top = {TOPIC};
            client.subscribe(top, Qos);
        }catch (Exception e){}
    }

    /**
     * */
    private void statistic(ArrayList<String> MessageStream, HashSet<String> DuplicateMessage){
        double DUPL_RATE = MessageStream.size()==0?0:DuplicateMessage.size()/(double)MessageStream.size();
        BigDecimal LOST_Gap = new BigDecimal("0");
        BigDecimal PRE_Message = new BigDecimal(MessageStream.get(0));
        for (int i = 1; i < MessageStream.size(); i++) {
            BigDecimal CUR_Message = new BigDecimal(MessageStream.get(i));
            LOST_Gap = LOST_Gap.add(CUR_Message.subtract(PRE_Message).subtract(new BigDecimal("1")));
            PRE_Message = CUR_Message;
        }
        BigDecimal TotalLength = new BigDecimal(MessageStream.size()+"");
        TotalLength = TotalLength.add(LOST_Gap);
        BigDecimal LOST_RATE = LOST_Gap.divide((TotalLength), 4, BigDecimal.ROUND_HALF_UP);
        LOST_RATE = LOST_RATE.multiply(new BigDecimal("100"));
        LOST_RATE = LOST_RATE.divide(new BigDecimal("1"), 2, BigDecimal.ROUND_HALF_UP);
        System.out.println("Total length: "+ MessageStream.size());
        System.out.println("Duplicate rate: " + (100 - (DUPL_RATE*100))+"%");
        System.out.println("Lost rate: "+ LOST_RATE.toString()+"%");
        System.out.println("Pot length: " + TotalLength.toString());
    }

    public void disconnect() throws MqttException {
        client.disconnect();
        client.close();
        statistic($MessageStream, $DuplicateMessage);
    }
}
