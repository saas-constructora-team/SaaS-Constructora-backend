-- Datos ampliados de captación del proyecto (formulario de registro).
ALTER TABLE projects
    ADD COLUMN contact_email             VARCHAR(200),
    ADD COLUMN contact_phone             VARCHAR(50),
    ADD COLUMN project_type              VARCHAR(30),
    ADD COLUMN property_use              VARCHAR(30),
    ADD COLUMN estimated_area            NUMERIC(12, 2),
    ADD COLUMN occupant_count            INTEGER,
    ADD COLUMN required_spaces           VARCHAR(1000),
    ADD COLUMN style_references          VARCHAR(2000),
    ADD COLUMN budget_range              VARCHAR(30),
    ADD COLUMN estimated_start_date      DATE,
    ADD COLUMN estimated_delivery_date   DATE,
    ADD COLUMN lot_ownership             VARCHAR(30),
    ADD COLUMN has_deeds                 BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_topographic_survey    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_previous_plans        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_site_photos           BOOLEAN NOT NULL DEFAULT FALSE;
