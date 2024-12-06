#!/bin/bash

# Check if the keystore password was passed as an argument
if [ -z "$1" ]; then
  echo "Error: Keystore password is required as an argument."
  echo "Usage: $0 <keystore-password>"
  exit 1
fi

# Define paths
CERT_PATH="/etc/letsencrypt/live/charge.humanfinance.com.br/fullchain.pem"
KEY_PATH="/etc/letsencrypt/live/charge.humanfinance.com.br/privkey.pem"
KEYSTORE_PATH="/etc/letsencrypt/live/charge.humanfinance.com.br/keystore.p12"
KEYSTORE_PASSWORD="$1"  # Accept password as the first argument
SPRING_BOOT_SERVICE_NAME="<your-spring-boot-service-name>"
SCRIPT_NAME="update-keystore.sh"  # Name of the deploy hook script
DEPLOY_HOOK_PATH="/etc/letsencrypt/renewal-hooks/deploy/$SCRIPT_NAME"

# Create the script to update the keystore
cat <<EOF > "$DEPLOY_HOOK_PATH"
#!/bin/bash

# Convert the certificate and private key to PKCS12 keystore format
sudo openssl pkcs12 -export -in "$CERT_PATH" -out "$KEYSTORE_PATH" -inkey "$KEY_PATH" -name tomcat -password pass:$KEYSTORE_PASSWORD
echo "Keystore updated."

# Restart the Spring Boot application to apply the new keystore
# systemctl restart $SPRING_BOOT_SERVICE_NAME
# echo "Spring Boot application restarted."
EOF

# Check if the deploy hook script already exists
if [ -f "$DEPLOY_HOOK_PATH" ]; then
  echo "Deploy hook script already exists, replacing it..."
else
  echo "Deploy hook script does not exist, creating it..."
fi

# Set the correct permissions for the deploy hook script
echo "Setting permissions for the deploy hook script..."
sudo chmod +x "$DEPLOY_HOOK_PATH"

# Confirmation message
echo "Deploy hook script created/replaced and permissions set."

echo "Script to update keystore created."