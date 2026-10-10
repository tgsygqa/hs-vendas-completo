#!/usr/bin/env bash
# Executado uma vez na criação do Codespace / dev container.
set -e

# Bibliotecas de sistema que o Cypress (Electron) precisa para rodar em Linux.
sudo apt-get update
sudo apt-get install -y --no-install-recommends \
  libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libnss3 libxss1 \
  libasound2 libxtst6 xauth xvfb \
  postgresql postgresql-contrib

# Dependências do projeto (Cypress, http-server, start-server-and-test).
npm install

# --- PostgreSQL: cria o banco e o usuario usados pelo backend -------------
# (ver backend/src/main/resources/application.properties). Instalado
# direto no container via apt, em vez de um container Docker separado via
# docker-compose, para nao depender de Docker-dentro-do-Codespace: menos
# uma camada que pode falhar.
sudo service postgresql start

sudo -u postgres psql -v ON_ERROR_STOP=1 <<-SQL
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'hardwarestore') THEN
      CREATE ROLE hardwarestore LOGIN PASSWORD 'hardwarestore';
    END IF;
  END
  \$\$;
SQL

sudo -u postgres psql -v ON_ERROR_STOP=1 -tc "SELECT 1 FROM pg_database WHERE datname = 'hardwarestore'" | grep -q 1 || \
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE hardwarestore OWNER hardwarestore;"

echo "PostgreSQL pronto: banco 'hardwarestore', usuario 'hardwarestore'."
