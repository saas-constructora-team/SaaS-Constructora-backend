-- Datos ampliados de la empresa (tenant) capturados en el registro de cuenta.
ALTER TABLE tenants
    ADD COLUMN trade_name    VARCHAR(150),
    ADD COLUMN tax_id        VARCHAR(50),
    ADD COLUMN contact_phone VARCHAR(50),
    ADD COLUMN address       VARCHAR(255),
    ADD COLUMN company_email VARCHAR(255);
