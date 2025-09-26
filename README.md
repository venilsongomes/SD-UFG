**Curso: Sistemas Distribuídos**<br>
**Docente: Vagner Sacramento**<br>
**Discente:Venilson Gomes Rocha - 201912649**


**Relatório Técnico: Desenvolvimento de um Sistema de Chat com Sockets**


**Introdução**<br>
Este relatório técnico detalha o desenvolvimento de um sistema de chat cliente-servidor em Java<br>
utilizando sockets para viabilizar a comunicação em rede. A aplicação implementada suporta funcionalidades essenciais,<br>
como troca de mensagens privadas, comunicação em grupos e transferência de arquivos, atendendo aos requisitos funcionais<br>
propostos para um ambiente de chat interativo e eficiente.

**Arquitetura e Decisões Técnicas**

1 - RF01 - Utilização de conexão cliente - servidor<br>
2 - RF02 - Enviar Mensagem para um cliente conectado<br>
3 - RF03 - Enviar arquivos para um cliente conectado ou grupo<br>
4 - RF04 - Criar grupo<br>
5 - RF05 -  Mandar mensagem a um grupo existente<br>
6 - RF06 - Se adicionar a um grupo existente.<br>

**Requisitos não Funcionais(RNF)**

RNF 01 -  Utilização de Socket para comunicação via rede<br>
RNF 02 - Tratamento de exceções<br>
RNF 03 -  Persistência de informações em base de dados<br>
RNF 04 - Autenticação de usuário pelo nome<br>
RNF 05 - Utilização da linguagem java.<br>

**Protocolo TCP (Transmission Control Protocol):** 

Foi motivada por sua natureza orientada à conexão e garantia de entrega de pacotes,<br>
características essenciais para uma aplicação de chat onde a perda de mensagens é indesejável.<br>
**Gerenciamento de concorrência Multithreading:** O servidor foi projetado para ser Multithreading <br>
a o receber uma nova requisição de clientes a thread principal do servidor cria e inicia uma nova thread, <br>
exclusiva a esse cliente.<br>
Isso permite que o servidor gerencie múltiplas conexões simultaneamente sem que um cliente bloqueie o atendimento aos outros.<br>
Para garantir a segurança no acesso a estruturas de dados compartilhadas (como os mapas de clientes e grupos).

**História de Usuários**




