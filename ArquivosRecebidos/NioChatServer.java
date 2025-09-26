import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NioChatServer {

    private static final int PORT = 55555;
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    // Estruturas para gerenciar o estado do chat
    // Usamos ConcurrentHashMap para segurança em futuras expansões com múltiplos workers
    private final Map<SocketChannel, String> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<SocketChannel>> groups = new ConcurrentHashMap<>();

    public NioChatServer() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        // Registra o canal do servidor para aceitar novas conexões
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("[*] Servidor de chat NIO iniciado na porta " + PORT);
    }

    public void start() {
        try {
            // Loop de eventos principal
            while (true) {
                // Bloqueia até que pelo menos um canal esteja pronto para uma operação
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }

                    // Remove a chave do conjunto para não processá-la novamente
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no loop do servidor: " + e.getMessage());
        } finally {
            closeServer();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        // A primeira mensagem do cliente será o seu nome de usuário
        System.out.println("[+] Nova conexão aceita de: " + clientChannel.getRemoteAddress());
        sendMessage(clientChannel, "Bem-vindo ao Chat! Por favor, digite seu nome de usuário:");
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead;

        try {
            bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                // Cliente desconectou
                handleDisconnect(key);
                return;
            }

            buffer.flip();
            String message = CHARSET.decode(buffer).toString().trim();

            if (!clients.containsKey(clientChannel)) {
                // A primeira mensagem é o nome de usuário
                registerUser(clientChannel, message);
            } else {
                // Processa mensagens normais
                processMessage(clientChannel, message);
            }
        } catch (IOException e) {
            // Erro na conexão, cliente provavelmente caiu
            handleDisconnect(key);
        }
    }

    private void registerUser(SocketChannel clientChannel, String username) throws IOException {
        if (clients.containsValue(username)) {
            sendMessage(clientChannel, "ERRO: Nome de usuário já existe. Desconectando.");
            clientChannel.close();
        } else {
            clients.put(clientChannel, username);
            System.out.println("[i] Usuário '" + username + "' registrado para " + clientChannel.getRemoteAddress());
            sendMessage(clientChannel, "Olá " + username + "! Você está conectado.");
        }
    }

    private void processMessage(SocketChannel senderChannel, String message) {
        String senderUsername = clients.get(senderChannel);
        System.out.println("[" + senderUsername + "]: " + message);

        if (message.startsWith("@")) {
            // Mensagem privada: @username:texto
            int separatorIndex = message.indexOf(':');
            if (separatorIndex > 1) {
                String recipientUsername = message.substring(1, separatorIndex);
                String privateMessage = message.substring(separatorIndex + 1).trim();
                sendMessageToUser(recipientUsername, "[" + senderUsername + " -> Você]: " + privateMessage);
            }
        } else if (message.startsWith("#")) {
            // Mensagem de grupo: #grupo:texto
            int separatorIndex = message.indexOf(':');
            if (separatorIndex > 1) {
                String groupName = message.substring(1, separatorIndex);
                String groupMessage = message.substring(separatorIndex + 1).trim();
                sendMessageToGroup(groupName, senderChannel, "[" + groupName + "] " + senderUsername + ": " + groupMessage);
            }
        } else if (message.startsWith("/creategroup ")) {
            // Comando para criar grupo: /creategroup nomegrupo
            String groupName = message.split(" ")[1];
            createGroup(senderChannel, groupName);
        } else {
            // Mensagem pública (não implementado neste exemplo, poderia ser um broadcast)
             sendMessage(senderChannel, "Comando inválido ou mensagem pública (não suportado).");
        }
    }

    private void createGroup(SocketChannel creatorChannel, String groupName) {
        groups.computeIfAbsent(groupName, k -> new HashSet<>()).add(creatorChannel);
        String creatorUsername = clients.get(creatorChannel);
        System.out.println("[i] Grupo '" + groupName + "' criado por '" + creatorUsername + "'.");
        sendMessage(creatorChannel, "Grupo '" + groupName + "' criado com sucesso.");
    }

    private void sendMessageToUser(String recipientUsername, String message) {
        for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(recipientUsername)) {
                sendMessage(entry.getKey(), message);
                return;
            }
        }
    }

    private void sendMessageToGroup(String groupName, SocketChannel senderChannel, String message) {
        Set<SocketChannel> groupMembers = groups.get(groupName);
        if (groupMembers != null) {
            // Adiciona o remetente ao grupo se ele não for membro
            groupMembers.add(senderChannel);
            for (SocketChannel memberChannel : groupMembers) {
                 sendMessage(memberChannel, message);
            }
        } else {
            sendMessage(senderChannel, "ERRO: Grupo '" + groupName + "' não existe.");
        }
    }

    private void sendMessage(SocketChannel clientChannel, String message) {
        try {
            ByteBuffer buffer = CHARSET.encode(message + "\n");
            clientChannel.write(buffer);
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem para " + clients.get(clientChannel) + ": " + e.getMessage());
            handleDisconnect(new SelectionKeyStub(clientChannel));
        }
    }

    private void handleDisconnect(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String username = clients.remove(clientChannel);
        if (username != null) {
            System.out.println("[-] Cliente '" + username + "' desconectado.");
            // Remove o usuário de todos os grupos
            groups.values().forEach(members -> members.remove(clientChannel));
        } else {
            System.out.println("[-] Conexão anônima perdida de " + getRemoteAddressSafe(clientChannel));
        }
        
        try {
            clientChannel.close();
            key.cancel();
        } catch (IOException e) {
            // Ignorar
        }
    }

    private String getRemoteAddressSafe(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "endereço desconhecido";
        }
    }

    private void closeServer() {
        try {
            selector.close();
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Classe stub para permitir a desconexão mesmo fora do handleRead
    private static class SelectionKeyStub extends SelectionKey {
        private final Channel channel;
        public SelectionKeyStub(Channel channel) { this.channel = channel; }
        @Override public SelectableChannel channel() { return (SelectableChannel) channel; }
        @Override public Selector selector() { return null; }
        @Override public int interestOps() { return 0; }
        @Override public SelectionKey interestOps(int ops) { return null; }
        @Override public int readyOps() { return 0; }
        @Override
        public boolean isValid() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isValid'");
        }
        @Override
        public void cancel() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'cancel'");
        }
    }

    public static void main(String[] args) {
        try {
            NioChatServer server = new NioChatServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Falha ao iniciar o servidor NIO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

