# Como testar o NEXUS sem perder seus dados

1. Abra o aplicativo e confirme que a tela inicial aparece.
2. Cadastre um cliente.
3. Cadastre um serviço.
4. Crie um projeto usando o cliente e o serviço.
5. Edite o prazo, marque como concluído e como pago.
6. Exporte os projetos para CSV.
7. Encerre pelo menu e abra novamente: os dados devem continuar lá.
8. Tente excluir um cliente usado em um projeto: o sistema deve impedir.

Para testes automatizados Java, use `03_RECOMPILAR_CODIGO.bat`, que roda os testes antes de gerar o JAR.
