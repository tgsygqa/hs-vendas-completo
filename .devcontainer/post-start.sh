#!/usr/bin/env bash
# Executado toda vez que o container inicia (inclusive ao retomar um
# Codespace parado) - o post-create.sh so roda uma vez, na criacao, e o
# Postgres nao fica de pe sozinho entre uma parada e outra do container.
sudo service postgresql start 2>/dev/null || true
