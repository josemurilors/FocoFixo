# FocoFixo – design (v1)

## Contexto
App Android para reduzir cinetose (enjoo de movimento) enquanto a pessoa usa o celular em veículo. O enjoo vem do conflito entre o que os olhos veem (tela parada) e o que o ouvido interno sente (movimento). Pontos de referência na tela que se movem com o veículo reduzem esse conflito (mesma ideia do "Vehicle Motion Cues" do iOS).

Decisões: função central = âncora visual; plataforma = Android nativo (Kotlin); forma = overlay sobre qualquer app.

## Fora de escopo (v1)
Exercícios guiados, diário de viagens, conta/login, backend, iOS.

## Arquitetura
- Kotlin + Jetpack Compose, módulo único, minSdk 26.
- `MainActivity` (Compose): botão liga/desliga, pedido da permissão "Exibir sobre outros apps" (`Settings.canDrawOverlays`), 3 ajustes (sensibilidade, tamanho dos pontos, opacidade) salvos em DataStore.
- `AnchorService` (foreground service, notificação com ação "Desligar"): cria a view de overlay via `WindowManager` com `TYPE_APPLICATION_OVERLAY`, flags `NOT_FOCUSABLE | NOT_TOUCHABLE` (toques passam ao app de baixo).
- `MotionSource`: lê `TYPE_LINEAR_ACCELERATION` (fallback `TYPE_ACCELEROMETER` com filtro passa-alta); expõe `Flow<Offset>` suavizado (passa-baixa).
- `AnchorView` (Canvas): ~8 pontos nas bordas esquerda/direita (centro livre); cada ponto se desloca no sentido oposto à aceleração, com mola de retorno ao centro.

## Fluxo de dados
sensor → `MotionSource` (suavização) → `AnchorView.offset` → redesenho ~60 fps. Ajustes do DataStore alimentam o serviço em tempo real.

## Erros
- Sem permissão de overlay: não inicia o serviço; abre a tela de configurações do sistema.
- Sem sensor de aceleração: mensagem clara; serviço não inicia.
- Serviço desregistra o listener do sensor em `onDestroy`.

## Testes
- Unitário (JVM): lógica pura de `MotionSource` (filtros, direção, limites) com entradas sintéticas.
- Manual em aparelho real: ligar overlay, abrir outro app, mover o aparelho; toques passam; desligar pela notificação funciona.

## Verificação
`./gradlew assembleDebug` e `./gradlew test`; instalar em aparelho e validar o fluxo manual.
