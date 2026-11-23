package pubsub;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.Base64;
/** Publicador: conhece somente o endereço do broker, nunca os clientes inscritos. */
public class PubSubPublisher {
    private final String host; private final int port;
    public PubSubPublisher(String host,int port){this.host=host;this.port=port;}
    public void publish(String topic,String payload) throws IOException {
        try(Socket s=new Socket(host,port);
            BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
            BufferedWriter out=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8))) {
            out.write("PUB|"+enc(topic)+"|"+enc(payload)+"\n"); out.flush();
            String reply=in.readLine(); if(reply==null || !reply.startsWith("OK|PUBLISHED")) throw new IOException("Broker não confirmou publicação: "+reply);
        }
    }
    private static String enc(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
}
