#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Starting Nginx and Certbot installation..."

# Update and install necessary packages
echo "Updating system and installing prerequisites..."
sudo apt update -y && sudo apt upgrade -y
sudo apt install -y nginx python3-certbot-nginx

# Enable and start Nginx
echo "Enabling and starting Nginx..."
sudo systemctl enable nginx
sudo systemctl start nginx

# Obtain SSL certificate without interactive prompts
EMAIL="borges.kauan.martins@gmail.com"  # Replace with your email address
DOMAIN="charge.humanfinance.com"  # Replace with your domain name

# Obtain SSL certificate
sudo certbot --nginx --non-interactive --agree-tos --email "$EMAIL" -d "$DOMAIN" -d "www.$DOMAIN"

sudo cp -f ~/ngin_config /etc/nginx/sites-available/default

# Test Nginx configuration
echo "Testing Nginx configuration..."
sudo nginx -t

# Reload Nginx to apply changes
echo "Reloading Nginx..."
sudo systemctl reload nginx

# Set up automatic SSL renewal
echo "Setting up automatic SSL renewal..."
sudo systemctl enable certbot.timer