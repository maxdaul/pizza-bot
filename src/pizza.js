function pizzaReset(s) {
    s.order = {size: null, topping: null, base: null, address: null, total: null};
    s.awaitingConfirmation = false;
    s.orderCompleted = false;
}

function pizzaEnsure(s) {
    if (!s.order) pizzaReset(s);
}

function pizzaMissing(o) {
    var fields = ["size", "topping", "base", "address"];
    var missing = [];
    for (var i = 0; i < fields.length; i++) {
        if (!o[fields[i]]) missing.push(fields[i]);
    }
    return missing;
}

function pizzaPrice(o) {
    return Number(o.size.price) + Number(o.topping.extra) + Number(o.base.extra);
}

function pizzaSave(s, p) {
    pizzaEnsure(s);
    if (s.orderCompleted) return false;
    var fields = ["size", "topping", "base", "address"];
    for (var i = 0; i < fields.length; i++) {
        var field = fields[i];
        var value = p["_" + field];
        if (value !== undefined && value !== null && value !== "") {
            s.order[field] = value;
        }
    }
    s.order.total = null;
    s.awaitingConfirmation = false;
    return true;
}
