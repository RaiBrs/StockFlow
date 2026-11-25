-- O índice impede códigos equivalentes mesmo em cadastros simultâneos.
CREATE UNIQUE INDEX warehouses_code_ignore_case_key ON warehouses (upper(code));
