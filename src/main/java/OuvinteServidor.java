public interface OuvinteServidor {
    default void aoEntrar(String apelido) {}
    default void aoSair(String apelido) {}
    default void aoMensagem(String apelido, String texto) {}
}