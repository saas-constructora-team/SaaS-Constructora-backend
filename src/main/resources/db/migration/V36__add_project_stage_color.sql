-- Color Sage que representa a cada etapa en el tablero kanban.
ALTER TABLE project_stages
    ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'SAGE';
