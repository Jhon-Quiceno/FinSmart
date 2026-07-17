ALTER TABLE expenses ADD COLUMN card_movement_id BIGINT;

-- ON DELETE SET NULL: eliminar un movimiento de tarjeta no debe eliminar el historial del gasto
-- que generó; el gasto conserva su registro pero pierde el vínculo con el movimiento.
ALTER TABLE expenses ADD CONSTRAINT fk_expenses_card_movement
    FOREIGN KEY (card_movement_id) REFERENCES card_movements (id) ON DELETE SET NULL;

CREATE INDEX idx_expenses_card_movement_id ON expenses (card_movement_id);
