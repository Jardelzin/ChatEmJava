import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServidorFX extends Application implements OuvinteServidor {
    private TextArea log = new TextArea();
    private ObservableList<String> usuarios = FXCollections.observableArrayList();
    private ServerSocket serverSocket;

    @Override
    public void start(Stage stage) {
        log.setEditable(false);
        log.setWrapText(true);

        Label tituloLista = new Label("Online");
        tituloLista.setStyle("-fx-font-weight: bold;");
        ListView<String> listaUsuarios = new ListView<>(usuarios);
        listaUsuarios.setPrefWidth(150);
        VBox.setVgrow(listaUsuarios, Priority.ALWAYS);
        VBox painelDireito = new VBox(6, tituloLista, listaUsuarios);

        BorderPane raiz = new BorderPane();
        raiz.setCenter(log);
        raiz.setRight(painelDireito);
        BorderPane.setMargin(painelDireito, new Insets(0, 0, 0, 10));
        raiz.setPadding(new Insets(10));

        stage.setTitle("Server");
        stage.setScene(new Scene(raiz, 550, 400));
        stage.show();

        ClienteHandler.setOuvinte(this); // "me inscrevo" para receber os avisos
        iniciarServidor();
    }

    private void iniciarServidor() {
        Thread aceitador = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                registrar("Servidor ouvindo na porta 5000...");
                while (true) {
                    Socket cliente = serverSocket.accept();
                    Thread t = new Thread(new ClienteHandler(cliente));
                    t.setDaemon(true);
                    t.start();
                }
            } catch (IOException e) {
                registrar("Servidor encerrado: " + e.getMessage());
            }
        });
        aceitador.setDaemon(true);
        aceitador.start();
    }

    // ---- Avisos vindos do ClienteHandler (chegam em OUTRAS threads) ----

    @Override
    public void aoEntrar(String apelido) {
        Platform.runLater(() -> usuarios.add(apelido));
        registrar(apelido + " entrou");
    }

    @Override
    public void aoSair(String apelido) {
        Platform.runLater(() -> usuarios.remove(apelido));
        registrar(apelido + " saiu");
    }

    @Override
    public void aoMensagem(String apelido, String texto) {
        registrar(apelido + ": " + texto);
    }

    private void registrar(String texto) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> log.appendText(hora + "  " + texto + "\n"));
    }

    @Override
    public void stop() throws IOException {
        if (serverSocket != null) serverSocket.close();
    }
}