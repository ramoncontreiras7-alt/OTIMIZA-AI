# OTIMIZA AI

Aplicativo Android para entregadores que trabalham com várias plataformas ao mesmo tempo
(Mercado Livre, Amazon Flex, iFood e outras). Junta a **roteirização** (melhor ordem das
paradas, km e tempo) com a **conta de ganhos** (lucro líquido, R$/km e R$/hora).

## Estado atual (Fase 0)

| Parte | Situação |
|---|---|
| `domain` — regras de negócio (paradas, plataformas, IDs, cálculo de lucro) | ✅ pronto e testado |
| `data` — banco de dados local (Room) | ✅ pronto, compila e tem testes |
| `app` — telas | ⏳ Fase 1 |
| Mapa, otimização de rota, scanner, avaliador de ofertas | ⏳ Fases 1 a 3 |

## Regra de ouro

O ID original de cada pedido (o que a plataforma fornece) **nunca** é alterado, cortado ou
substituído. A parada é identificada pelo par **ID original + plataforma**.

## Como o projeto está organizado

```
domain/   Kotlin puro. Não conhece Android. Regras e contas.
  model/      DeliveryStop, Platform, Identifiers, VehicleProfile, RouteEconomics...
  usecase/    CalculateRouteProfitUseCase (lucro, R$/km, R$/hora)
  repository/ contrato de acesso às paradas
data/     Implementação com banco local Room + injeção com Hilt.
```

## Testes

Rodam sozinhos no GitHub a cada envio (aba **Actions**). Para rodar no PC:

```
gradlew.bat :domain:test :data:testDebugUnitTest
```

## Decisão de produto

O app **não** aceita rotas automaticamente (autoclique). Isso viola os termos das plataformas
e pode desativar a conta do entregador. No lugar, haverá um **avaliador de ofertas** que só
mostra se a oferta vale a pena, e a decisão de aceitar continua com o entregador.
