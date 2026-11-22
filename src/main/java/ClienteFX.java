import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClienteFX extends Application {
    private Socket socket;
    private PrintWriter saida;
    private TextArea areaMensagens;
    private TextField campoMensagem;
    private Circle indicador;
    private Label rotuloStatus;
    private String apelido;

    @Override
    public void start(Stage stage) {
        // 1. Pergunta o apelido
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Login");
        dialogo.setHeaderText("Digite seu apelido");
        apelido = dialogo.showAndWait().orElse("").trim();
        if (apelido.isEmpty()) {
            Platform.exit();
            return;
        }

        // 2. Cabeçalho: círculo de status + status + usuário
        indicador = new Circle(7, Color.GRAY);
        rotuloStatus = new Label("Connecting...");
        rotuloStatus.setStyle("-fx-text-fill: #ff5555; -fx-font-weight: bold;");

        Label rotuloUsuario = new Label("User: " + apelido);
        rotuloUsuario.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox cabecalho = new HBox(8, indicador, rotuloStatus, espaco, rotuloUsuario);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        // 3. Área de mensagens
        areaMensagens = new TextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setWrapText(true);
        VBox.setVgrow(areaMensagens, Priority.ALWAYS);

        // 4. Linha de envio
        campoMensagem = new TextField();
        campoMensagem.setPromptText("Enter message...");
        HBox.setHgrow(campoMensagem, Priority.ALWAYS);

        Button botaoEnviar = new Button("SEND");
        botaoEnviar.setStyle(
                "-fx-background-color: #0d3b8c; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        HBox linhaEnvio = new HBox(8, campoMensagem, botaoEnviar);

        // 5. Monta tudo
        VBox raiz = new VBox(8, cabecalho, areaMensagens, linhaEnvio);
        raiz.setPadding(new Insets(10));
        raiz.setStyle("-fx-background-color: #1f5fbf;");

        botaoEnviar.setOnAction(e -> enviar());
        campoMensagem.setOnAction(e -> enviar());

        stage.setTitle("Client - " + apelido);
        stage.setScene(new Scene(raiz, 400, 400));
        stage.show();

        conectar();
    }

    private void conectar() {
        try {
            socket = new Socket("localhost", 5000);
            saida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            saida.println(apelido);
            definirStatus(true);

            Thread ouvinte = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = entrada.readLine()) != null) {
                        final String texto = msg;
                        Platform.runLater(() ->
                                areaMensagens.appendText(texto + "\n"));
                    }
                } catch (IOException e) {
                    // conexão fechada
                }
                Platform.runLater(() -> {
                    areaMensagens.appendText("--- Conexão encerrada ---\n");
                    definirStatus(false);
                });
            });
            ouvinte.setDaemon(true);
            ouvinte.start();

        } catch (IOException e) {
            definirStatus(false);
            Alert alerta = new Alert(Alert.AlertType.ERROR,
                    "Não consegui conectar. O servidor está rodando?");
            alerta.showAndWait();
            Platform.exit();
        }
    }

    // Troca a cor do círculo e o texto de status
    private void definirStatus(boolean conectado) {
        if (conectado) {
            indicador.setFill(Color.LIMEGREEN);
            rotuloStatus.setText("Connected");
            rotuloStatus.setStyle("-fx-text-fill: #7CFC00; -fx-font-weight: bold;");
        } else {
            indicador.setFill(Color.RED);
            rotuloStatus.setText("Disconnected");
            rotuloStatus.setStyle("-fx-text-fill: #ff5555; -fx-font-weight: bold;");
        }
    }

    private void enviar() {
        String texto = campoMensagem.getText().trim();
        if (texto.isEmpty()) return;

        saida.println(texto);
        areaMensagens.appendText("Você: " + texto + "\n");
        campoMensagem.clear();
    }

    @Override
    public void stop() throws IOException {
        if (socket != null) socket.close();
    }
}