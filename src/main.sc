require: pizza.js

theme: /

    state: Start
        q!: $regex</start>
        intent!: /НачатьЗаново
        script:
            pizzaReset($session);
        a: Добро пожаловать! Выберите размер пиццы: маленькая, средняя или большая; начинку: мясная, сырная или овощная; основу: тонкая или пышная. Можно указать несколько параметров сразу.
        a: Доставляем на улицы Ленина, Лермонтова и Советская. Формат адреса: улица Ленина дом 12. Доставка бесплатная.
        go!: /CheckOrder

    state: Hello
        intent!: /привет
        a: Привет, я помогу тебе заказать пиццу! Для начала напишите /start.
        go!: /CheckOrder

    state: GetOrder
        intent!: /ЗаказатьПиццу
        go!: /SaveParameters

    state: GetSize
        intent!: /ВыбратьРазмер
        go!: /SaveParameters

    state: GetTopping
        intent!: /ВыбратьНачинку
        go!: /SaveParameters

    state: GetBase
        intent!: /ВыбратьОснову
        go!: /SaveParameters

    state: GetAddress
        intent!: /УказатьАдрес
        go!: /SaveParameters

    state: SaveParameters
        script:
            if (pizzaSave($session, $parseTree || {})) {
                $reactions.transition("/CheckOrder");
            } else {
                $reactions.transition("/Finished");
            }

    state: CheckOrder
        script:
            pizzaEnsure($session);
            $session.awaitingConfirmation = false;
            if ($session.orderCompleted) {
                $reactions.transition("/Finished");
            } else {
                var missing = pizzaMissing($session.order);
                if (missing.length === 0) {
                    $reactions.transition("/ConfirmOrder");
                } else {
                    var questions = {
                        size: "Какой размер выбрать: маленькая, средняя или большая?",
                        topping: "Какую начинку выбрать: мясная, сырная или овощная?",
                        base: "Какую основу выбрать: тонкая или толстая?",
                        address: "Куда доставить? Напишите, например: улица Ленина дом 12. Доступны также улицы Лермонтова и Советская."
                    };
                    var field = missing[Math.floor(Math.random() * missing.length)];
                    $reactions.answer(questions[field]);
                }
            }

    state: ConfirmOrder
        script:
            pizzaEnsure($session);
            if ($session.orderCompleted) {
                $reactions.transition("/Finished");
            } else if (pizzaMissing($session.order).length > 0) {
                $reactions.transition("/CheckOrder");
            } else {
                var o = $session.order;
                o.total = pizzaPrice(o);
                $session.awaitingConfirmation = true;
                $reactions.answer("Ваш заказ: одна пицца.\nРазмер: " + o.size.ru_name +
                    ".\nНачинка: " + o.topping.ru_name + ".\nОснова: " + o.base.ru_name +
                    ".\nАдрес: " + o.address + ".\nИтого: " + o.total +
                    " руб. Доставка бесплатная.\nПодтвердите заказ. Напишите 'да' или скажите что хотели бы изменить");
            }

        state: Yes
            intent: /ПодтвердитьЗаказ
            go!: /CompleteOrder

        state: No
            intent: /ИзменитьЗаказ
            go!: /AskChanges

    state: ChangeOrder
        intent!: /ИзменитьЗаказ
        go!: /AskChanges

    state: AskChanges
        script:
            pizzaEnsure($session);
            $session.awaitingConfirmation = false;
            if ($session.orderCompleted) {
                $reactions.transition("/Finished");
            } else {
                var query = String($request.query || "").toLowerCase();
                var field = null;
                if (query.indexOf("размер") >= 0) field = "size";
                else if (query.indexOf("начин") >= 0) field = "topping";
                else if (query.indexOf("основ") >= 0 || query.indexOf("тест") >= 0) field = "base";
                else if (query.indexOf("адрес") >= 0) field = "address";
                var prompts = {
                    size: "Укажите новый размер: маленькая, средняя или большая.",
                    topping: "Укажите новую начинку: мясная, сырная или овощная.",
                    base: "Укажите новую основу: тонкая или пышная.",
                    address: "Укажите новый адрес, например: улица Лермонтова дом 7."
                };
                $reactions.answer(field ? prompts[field] :
                    "Заказ пока не оформлен. Укажите новый размер, начинку, основу или адрес. Например: «Поменяйте размер на маленькую». Для полного сброса: /start.");
            }

    state: ConfirmOutside
        intent!: /ПодтвердитьЗаказ
        a: Сначала проверим состав заказа. Оформление возможно после показа итоговой суммы и вопроса о подтверждении.
        go!: /CheckOrder

    state: CompleteOrder
        script:
            pizzaEnsure($session);
            if ($session.orderCompleted) {
                $reactions.transition("/Finished");
            } else if (!$session.awaitingConfirmation || pizzaMissing($session.order).length > 0) {
                $reactions.transition("/CheckOrder");
            } else {
                $session.order.total = pizzaPrice($session.order);
                $session.awaitingConfirmation = false;
                $session.orderCompleted = true;
                $reactions.answer("Заказ оформлен! Стоимость: " + $session.order.total +
                    " руб. Адрес: " + $session.order.address +
                    "Приятного аппетита.");
                $reactions.transition("/Finished");
            }

    state: Finished
        a: Для нового заказа напишите /start или «Начать заново».

    state: Bye
        intent!: /пока
        script:
            $session.awaitingConfirmation = false;
        a: До свидания! Чтобы начать новый заказ, напишите /start.

    state: NoMatch
        event!: noMatch
        a: Не удалось распознать сообщение. Можно указать размер, начинку, основу или адрес. Например: «Хочу большую мясную пиццу».
        go!: /CheckOrder
