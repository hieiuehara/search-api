package com.vivareal.search.api.parser;

public class Filter {

    private Field field;
    private RelationalOperator relationalOperator;
    private Value value;

    public Filter(Object[] expression) {
        // TODO array validation?
        this((Field) expression[0], (RelationalOperator) expression[1], (Value) expression[2]);
    }

    public Filter(Field field, RelationalOperator expression, Value value) {
        this.field = field;
        this.relationalOperator = expression;
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public RelationalOperator getRelationalOperator() {
        return relationalOperator;
    }

    public void setRelationalOperator(RelationalOperator relationalOperator) {
        this.relationalOperator = relationalOperator;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return field.getName() + " " + relationalOperator.toString() + " " + value.getContent();
    }
}
