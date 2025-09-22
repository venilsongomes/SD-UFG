import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe principal do servidor de chat.
 * Aceita conexões de clientes e cria uma thread ClientHandler para cada um.
 * Gerencia clientes e grupos de forma segura para múltiplas threads.
 */
public class ChatServer {
    
    private static final int PORT = 55555;
    private static final String IP_ADDRESS = "127.0.0.1";
    // Mapa de nome de usuário para seu ClientHandler (thread do cliente)
    // ConcurrentHashMap garante segurança em ambiente multithread
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // Mapa de nome do grupo para lista de membros (ClientHandler)
    private static final Map<String, List<ClientHandler>> groups = new ConcurrentHashMap<>();

    // Método principal do servidor
    public static void main(String[] args) {
        // Exibe mensagem de inicialização
        System.out.println("Servidor de chat iniciando na porta " + PORT + " e IP " + IP_ADDRESS + "...");

        // Tenta abrir o socket do servidor na porta definida
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, java.net.InetAddress.getByName(IP_ADDRESS))) {
            // Loop infinito para aceitar conexões de clientes
            while (true) {
                // Aceita uma nova conexão de cliente
                Socket clientSocket = serverSocket.accept();
                // Cria um ClientHandler para o novo cliente
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // Inicia a thread do ClientHandler
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            // Exibe erro caso não consiga iniciar o servidor
            System.err.println("Não foi possível iniciar o servidor na porta " + PORT);
            e.printStackTrace();
        }
    }

    // --- Métodos de Gerenciamento (usados pelos ClientHandlers) ---

    // Adiciona um cliente ao mapa de clientes
    public static void addClient(String username, ClientHandler handler) {
        clients.put(username, handler); // Adiciona ao mapa
        // Exibe mensagem de conexão
        System.out.println(
                "[CONEXÃO] " + username + " conectou-se de " + handler.getSocket().getInetAddress().getHostAddress());
    }

    // Remove um cliente do mapa de clientes e dos grupos
    public static void removeClient(ClientHandler handler) {
        String username = handler.getUsername(); // Obtém o nome do usuário
        if (username != null) {
            // Exibe mensagem de desconexão
            System.out.println("[DESCONEXÃO] " + username + " desconectou-se.");
            clients.remove(username); // Remove do mapa de clientes
            // Remove o cliente de todos os grupos em que participa
            for (String groupName : groups.keySet()) {
                List<ClientHandler> members = groups.get(groupName);
                // Remove o handler da lista de membros
                if (members.remove(handler) && members.isEmpty()) {
                    // Se o grupo ficou vazio, remove o grupo
                    groups.remove(groupName);
                    System.out.println("[INFO] Grupo '" + groupName + "' ficou vazio e foi removido.");
                }
            }
        }
    }

    // Retorna o ClientHandler de um usuário pelo nome
    public static ClientHandler getClient(String username) {
        return clients.get(username);
    }

    // Cria um novo grupo com o nome especificado
    public static void createGroup(String groupName, ClientHandler creator) {
        if (!groups.containsKey(groupName)) { // Se o grupo não existe
            // Cria lista sincronizada de membros
            List<ClientHandler> members = Collections.synchronizedList(new ArrayList<>());
            members.add(creator); // Adiciona o criador
            groups.put(groupName, members); // Adiciona ao mapa de grupos
            creator.sendMessage("[INFO] Grupo '" + groupName + "' criado com sucesso!");
        } else {
            // Grupo já existe
            creator.sendMessage("[ERRO] Grupo '" + groupName + "' já existe.");
        }
    }

    // Adiciona um usuário a um grupo existente
    public static void joinGroup(String groupName, ClientHandler user) {
        List<ClientHandler> members = groups.get(groupName); // Obtém lista de membros
        if (members != null) {
            if (!members.contains(user)) { // Se não é membro ainda
                members.add(user); // Adiciona ao grupo
                user.sendMessage("[INFO] Você entrou no grupo '" + groupName + "'.");
            } else {
                // Já é membro
                user.sendMessage("[INFO] Você já é membro do grupo '" + groupName + "'.");
            }
        } else {
            // Grupo não existe
            user.sendMessage("[ERRO] Grupo '" + groupName + "' não encontrado.");
        }
    }

    // Roteia mensagem para todos os membros de um grupo (exceto o remetente)
    public static void routeGroupMessage(String groupName, String message, ClientHandler sender) {
        List<ClientHandler> members = groups.get(groupName); // Obtém lista de membros
        if (members != null && members.contains(sender)) { // Se grupo existe e remetente é membro
            // Formata mensagem de grupo
            String formattedMsg = "[GRUPO " + groupName + " de " + sender.getUsername() + "]: " + message;
            // Itera sobre uma cópia da lista para evitar erro de concorrência
            for (ClientHandler member : new ArrayList<>(members)) { // Itera sobre uma cópia
                if (member != sender) { // Não envia para o remetente
                    member.sendMessage(formattedMsg); // Envia mensagem
                }
            }
        } else {
            // Grupo não existe ou remetente não é membro
            sender.sendMessage("[ERRO] Você não pode enviar mensagem para o grupo '" + groupName + "'.");
        }
    }

    // Roteia arquivo para todos os membros de um grupo (exceto o remetente)
    public static void routeGroupFile(String groupName, String filename, byte[] fileData, ClientHandler sender) {
        List<ClientHandler> members = groups.get(groupName); // Obtém lista de membros
        if (members != null && members.contains(sender)) { // Se grupo existe e remetente é membro
            // Exibe mensagem de roteamento de arquivo
            System.out.println("[ARQUIVO] Roteando '" + filename + "' para o grupo " + groupName);
            // Itera sobre uma cópia da lista para evitar erro de concorrência
            for (ClientHandler member : new ArrayList<>(members)) { // Itera sobre uma cópia
                if (member != sender) { // Não envia para o remetente
                    member.sendFile(sender.getUsername(), filename, fileData); // Envia arquivo
                }
            }
        } else {
            // Grupo não existe ou remetente não é membro
            sender.sendMessage("[ERRO] Grupo '" + groupName + "' inválido para envio de arquivo.");
        }
    }
}
