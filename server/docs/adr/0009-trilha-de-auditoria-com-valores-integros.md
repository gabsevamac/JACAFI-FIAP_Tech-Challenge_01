# 0009 — Trilha de auditoria com valores íntegros, placa inclusive

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Os pontos quentes **HS7 a HS10** do Event Storming registram ambiguidade sobre mutabilidade
de placa e sobre o motivo de uma alteração. A posição do grupo é que uma trilha detalhada
permite recuperar a informação correta depois do fato — o que exige responder *"qual placa
este veículo tinha em 12/03"*, e essa pergunta **não é respondível sem guardar o valor**.

Havia uma contradição com decisão já commitada. A migração da fatia `vehicle` afirmava, em
comentário SQL:

> *Referencia o veiculo por identificador e NUNCA guarda a placa. Uma trilha que copiasse o
> dado pessoal que ela existe para vigiar manteria esse dado vivo depois da remocao que
> deveria te-lo apagado.*

E `Vehicle.remove()` apaga a placa, substituindo-a por token irreversível, para atender ao
Art. 18, VI.

A segunda metade daquela afirmação é verdadeira. A questão é se é defeito ou consequência
aceita.

## Decisão

`audit_trail` guarda `old_value` e `new_value` **íntegros**, placa e CPF/CNPJ inclusive. Não
são mascarados: uma trilha registrando *"a placa mudou de \*\*\* para \*\*\*"* não responde
a única pergunta que motiva sua existência.

**Base legal da retenção: Art. 16, I** — conservação para cumprimento de obrigação legal ou
regulatória. O direito à eliminação do Art. 18, VI **se ressalva expressamente** às
hipóteses do Art. 16, então a trilha sobrevive à remoção do veículo.

**Consequência assumida, não defeito:** a remoção de veículo deixa de eliminar a placa do
sistema. Ela continua apagando `vehicles.license_plate` — o que libera o índice único
parcial para recadastro — mas o valor anterior permanece na trilha. A afirmação da migração
anterior fica corrigida por este registro.

A gravação é **responsabilidade do caso de uso**, não de listener de JPA. Um listener vê um
valor mudar e não sabe dizer *por quê*, e o porquê é o que importa: corrigir um erro de
digitação e registrar uma troca real de placa são os mesmos dois valores com significados
opostos. É HS9, e só o caso de uso está em posição de responder.

`reason` é nulável **por decisão**: HS9 registra que a semântica do motivo ainda é ambígua,
e exigir preenchimento agora produziria um campo cheio de "atualização".

## Alternativas consideradas

**Íntegro exceto campos `@PersonalData`.** Registraria que a placa mudou, sem o valor.
Preservaria o apagamento e **perderia HS7–HS10**, que é o caso de uso que motiva a trilha
por campo existir.

**Íntegro mais expurgo por titular.** Guardar tudo e apagar as entradas daquele titular
quando ele exerce o Art. 18. Fecha o buraco de fato, ao custo de código que o MVP não
precisa — e o Art. 16, I já dispensa a eliminação.

**Trilha por operação, como a de `vehicle`.** É a que já existe e continua existindo: as
duas coexistem porque respondem perguntas diferentes. `vehicle_audit_entries` responde
"quem mexeu neste veículo e quando"; `audit_trail` responde "qual era o valor".

## Consequências

**Obrigações operacionais que precisam constar da política de privacidade:**

- a tabela entra na política de retenção e **precisa de prazo definido**;
- um pedido de titular (Art. 18) alcança estas linhas e **precisa de procedimento**;
- os valores **nunca** podem ser logados nem devolvidos por API sem passar pelo `Masker`.

Sem chave estrangeira para nenhuma tabela de negócio, de propósito: prova que desaparece
junto com o dado que descreve não é prova.

O caráter *append-only* é regra de aplicação — a porta só oferece `record` — e **não**
restrição do schema. Endurecer no banco exigiria revogar `UPDATE` e `DELETE` do usuário da
aplicação, o que fica registrado como endurecimento para produção.
