import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Scanner;

public class ServidorOrcamento {

    private static final String EMAIL_DESTINO = "mario@onebiotech.com.br";
    private static final String REMETENTE_EMAIL = "comercial@onebiotech.com.br";
    private static final String REMETENTE_SENHA_APP = "OneComerci@l2026";

    public static void main(String[] args) throws Exception {
        // Servidor criado na porta 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/contato", new ContatoHandler());
        server.setExecutor(null);
        System.out.println("Servidor de Orçamentos rodando em [http://127.0.0.1:8080/api/contato](http://127.0.0.1:8080/api/contato)");
        server.start();
    }

    static class ContatoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Configuração de CORS completa
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                    String jsonBody = scanner.hasNext() ? scanner.next() : "";

                    // Dispara o e-mail
                    enviarEmail(jsonBody);

                    String resposta = "Orçamento enviado com sucesso!";
                    byte[] respostaBytes = resposta.getBytes("UTF-8");
                    
                    exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(200, respostaBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(respostaBytes);
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    String erro = "Erro no servidor: " + e.getMessage();
                    byte[] erroBytes = erro.getBytes("UTF-8");
                    exchange.sendResponseHeaders(500, erroBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(erroBytes);
                    os.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void enviarEmail(String conteudo) throws Exception {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            // Ajustado para o servidor Microsoft/Outlook (Altere se usarem outro provedor)
            props.put("mail.smtp.host", "smtp.office365.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(REMETENTE_EMAIL, REMETENTE_SENHA_APP);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(REMETENTE_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_DESTINO));
            message.setSubject("Solicitação de Orçamento - One Biotech");
            message.setText("Nova solicitação de orçamento recebida:\n\n" + conteudo);

            Transport.send(message);
        }
    }
}