#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  Генерация набора сертификатов для mTLS: CA, сервер, клиент.
#  Запуск:  cd MutualTls/certs && ./generate.sh
#  Результат: ca.pem/ca.key, server.pem/server.key, client.pem/client.key
# ---------------------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")"

DAYS_CA=1095   # 3 года
DAYS=825       # чуть больше 2 лет

echo "==> 1/3  Корневой CA"
openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out ca.key
openssl req -new -x509 -utf8 -config ca.cnf -key ca.key -days "$DAYS_CA" -out ca.pem

for name in server client; do
  echo "==> ${name}"
  # Ключ (PKCS#8, «BEGIN PRIVATE KEY») и запрос на сертификат (CSR)
  openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "${name}.key"
  openssl req -new -utf8 -config "${name}.cnf" -key "${name}.key" -out "${name}.csr"
  # CA подписывает CSR и добавляет расширения из секции <name>_ext
  openssl x509 -req -in "${name}.csr" -CA ca.pem -CAkey ca.key -CAcreateserial \
          -days "$DAYS" \
          -extfile "${name}.cnf" -extensions "${name}_ext" -out "${name}.pem"
  rm -f "${name}.csr"
done

echo "==> Проверка цепочек"
openssl verify -CAfile ca.pem server.pem client.pem

echo "==> Готово:"
for f in ca server client; do
  openssl x509 -in "${f}.pem" -noout -nameopt utf8,sep_comma_plus -subject -issuer -enddate
done
