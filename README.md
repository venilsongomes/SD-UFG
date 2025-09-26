**Curso: Sistemas Distribuídos**:
**Docente: Vagner Sacramento**:
**Discente:Venilson Gomes Rocha - 201912649**:


**Relatório Técnico: Desenvolvimento de um Sistema de Chat com Sockets**


**Introdução**
Este relatório técnico detalha o desenvolvimento de um sistema de chat cliente-servidor em Java,:
utilizando sockets para viabilizar a comunicação em rede. A aplicação implementada suporta funcionalidades essenciais,:
como troca de mensagens privadas, comunicação em grupos e transferência de arquivos, atendendo aos requisitos funcionais:
propostos para um ambiente de chat interativo e eficiente.

**Arquitetura e Decisões Técnicas**

1 - RF01 - Utilização de conexão cliente - servidor: 
2 - RF02 - Enviar Mensagem para um cliente conectado:
3 - RF03 - Enviar arquivos para um cliente conectado ou grupo:
4 - RF04 - Criar grupo:
5 - RF05 -  Mandar mensagem a um grupo existente:
6 - RF06 - Se adicionar a um grupo existente.:

**Requisitos não Funcionais(RNF)**

RNF 01 -  Utilização de Socket para comunicação via rede:
RNF 02 - Tratamento de exceções:
RNF 03 -  Persistência de informações em base de dados:
RNF 04 - Autenticação de usuário pelo nome:
RNF 05 - Utilização da linguagem java.

**Protocolo TCP (Transmission Control Protocol):** 

Foi motivada por sua natureza orientada à conexão e garantia de entrega de pacotes,:
características essenciais para uma aplicação de chat onde a perda de mensagens é indesejável.:
**Gerenciamento de concorrência Multithreading:** O servidor foi projetado para ser Multithreading a o receber uma nova requisição de clientes,:
a thread principal do servidor cria e inicia uma nova thread, exclusiva a esse cliente.:
Isso permite que o servidor gerencie múltiplas conexões simultaneamente sem que um cliente bloqueie o atendimento aos outros.:
Para garantir a segurança no acesso a estruturas de dados compartilhadas (como os mapas de clientes e grupos).:

**História de Usuários**




