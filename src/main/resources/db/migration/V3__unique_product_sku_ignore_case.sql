-- A consulta prévia não evita corridas; o índice também impede SKUs iguais em cadastros simultâneos.
CREATE UNIQUE INDEX products_sku_ignore_case_key ON products (upper(sku));
