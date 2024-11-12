#!/bin/bash

# Check if the correct number of arguments is provided
if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <path_to_pem_file> <ec2_hostname> <keystore_password>"
  exit 1
fi

# Assign arguments to variables
PEM_FILE=$1
EC2_HOST=$2
KEYSTORE_PASSWORD=$3

# Step 1: Transfer files using SCP
echo "Transferring files to EC2 instance..."
#scp -i "$PEM_FILE" target/chatbot.jar "$EC2_HOST":~/
scp -i "$PEM_FILE" setup/ec2/setup_keystore.sh "$EC2_HOST":~/

# Step 2: SSH into the EC2 instance and execute commands
echo "SSHing into EC2 instance and executing setup..."
ssh -i "$PEM_FILE" "$EC2_HOST" << EOF
    # Make sure the keystore script is executable
    chmod +x ~/setup_keystore.sh

    # Execute the setup script with the keystore password
    sudo ~/setup_keystore.sh "$KEYSTORE_PASSWORD"

    # Run the chatbot application (adjust if needed)
    # sudo setsid nohup java -jar ~/chatbot.jar > /dev/null 2>&1 &

    exit
EOF

echo "SSH session closed."