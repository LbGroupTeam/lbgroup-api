-- MySQL dump 10.13  Distrib 8.0.39, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: human_finance
-- ------------------------------------------------------
-- Server version	9.0.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `chargepoint_charging_data_relation`
--

DROP TABLE IF EXISTS `chargepoint_charging_data_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chargepoint_charging_data_relation` (
  `charging_data_id` bigint NOT NULL,
  `chargepoint_id` bigint NOT NULL,
  PRIMARY KEY (`charging_data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chargepoint_charging_data_relation`
--

LOCK TABLES `chargepoint_charging_data_relation` WRITE;
/*!40000 ALTER TABLE `chargepoint_charging_data_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `chargepoint_charging_data_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chargepoints`
--

DROP TABLE IF EXISTS `chargepoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chargepoints` (
  `ID_CHARGE` int NOT NULL AUTO_INCREMENT,
  `LOCAL_CHARGE` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `OWNER_CHARGER` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `ACTIVATION_DATE_CHARGER` date NOT NULL,
  `RUA_CHARGER` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `BAIRRO_CHARGER` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `CIDADE_CHARGER` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NUM_CHARGER` int NOT NULL,
  `CEP_CHARGER` int NOT NULL,
  `COMPLEMENTO_CHARGER` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `nome_amigavel` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operation_mode` enum('AUTOMATIC_OCPP','DISABLED','MANUAL') COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_CHARGE`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chargepoints`
--

LOCK TABLES `chargepoints` WRITE;
/*!40000 ALTER TABLE `chargepoints` DISABLE KEYS */;
INSERT INTO `chargepoints` VALUES (1,'PALM','SIMPARK','2024-04-02','Av. Francisco de Paula Leite','Recreio Campestre Jóia','Indaiatuba',3027,13346615,'NDA',NULL,'DISABLED'),(2,'20240003','SIMPARK','2024-05-17','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'DIREITA','ECO Vaga','AUTOMATIC_OCPP'),(3,'20240001','SIMPARK','2024-05-17','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA','ECO Vaga','AUTOMATIC_OCPP'),(4,'20240004','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'DIREITA','BMW','AUTOMATIC_OCPP'),(5,'20240002','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA','BMW','AUTOMATIC_OCPP'),(6,'20240003','SIMPARK','2024-05-17','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'DIREITA',NULL,'DISABLED'),(7,'20240001','SIMPARK','2024-05-17','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA',NULL,'DISABLED'),(8,'IGUATEMIDBMW','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'DIREITA',NULL,'DISABLED'),(9,'IGUATEMIEBMW','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA',NULL,'DISABLED'),(10,'SKYTRADECENTER','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA',NULL,'DISABLED'),(11,'SIMPARKMATRIZ','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA',NULL,'DISABLED'),(12,'MERCADAOCAMPOLIM','SIMPARK','2024-06-19','Av. Gisele Constantino','Parque Bela Vista','Votorantim',1850,18110650,'ESQUERDA',NULL,'DISABLED');
/*!40000 ALTER TABLE `chargepoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `charging_data`
--

DROP TABLE IF EXISTS `charging_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `charging_data` (
  `id` bigint NOT NULL,
  `energy_delivered_in_watts` double NOT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `stopped_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `charging_data`
--

LOCK TABLES `charging_data` WRITE;
/*!40000 ALTER TABLE `charging_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `charging_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `charging_data_seq`
--

DROP TABLE IF EXISTS `charging_data_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `charging_data_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `charging_data_seq`
--

LOCK TABLES `charging_data_seq` WRITE;
/*!40000 ALTER TABLE `charging_data_seq` DISABLE KEYS */;
INSERT INTO `charging_data_seq` VALUES (201);
/*!40000 ALTER TABLE `charging_data_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ocpptransaction_charging_data_relation`
--

DROP TABLE IF EXISTS `ocpptransaction_charging_data_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ocpptransaction_charging_data_relation` (
  `charging_data_id` bigint NOT NULL,
  `transaction_id` bigint NOT NULL,
  PRIMARY KEY (`charging_data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ocpptransaction_charging_data_relation`
--

LOCK TABLES `ocpptransaction_charging_data_relation` WRITE;
/*!40000 ALTER TABLE `ocpptransaction_charging_data_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ocpptransaction_charging_data_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` bigint NOT NULL,
  `amount` double NOT NULL,
  `reason` enum('EV_CHARGE') DEFAULT NULL,
  `reason_data` varchar(255) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','PENDING') DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_seq`
--

DROP TABLE IF EXISTS `payment_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_seq`
--

LOCK TABLES `payment_seq` WRITE;
/*!40000 ALTER TABLE `payment_seq` DISABLE KEYS */;
INSERT INTO `payment_seq` VALUES (201);
/*!40000 ALTER TABLE `payment_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pricescharge`
--

DROP TABLE IF EXISTS `pricescharge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricescharge` (
  `ID_PRICES` int NOT NULL AUTO_INCREMENT,
  `TYPE_CLIENT_PRICES` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `OWNER_PRICES` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `MULTIPLICATOR_PRICES` float(10,2) NOT NULL,
  PRIMARY KEY (`ID_PRICES`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pricescharge`
--

LOCK TABLES `pricescharge` WRITE;
/*!40000 ALTER TABLE `pricescharge` DISABLE KEYS */;
INSERT INTO `pricescharge` VALUES (1,'VIP','SIMPARK',1.49),(2,'OURO','SIMPARK',1.99),(3,'PRATA','SIMPARK',2.20),(4,'VIP','NES',1.99),(5,'OURO','NES',2.49),(6,'PRATA','NES',2.99),(7,'ADMIN','SIMPARK',1.49);
/*!40000 ALTER TABLE `pricescharge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_charging_data_relation`
--

DROP TABLE IF EXISTS `user_charging_data_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_charging_data_relation` (
  `charging_data_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`charging_data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_charging_data_relation`
--

LOCK TABLES `user_charging_data_relation` WRITE;
/*!40000 ALTER TABLE `user_charging_data_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_charging_data_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usu`
--

DROP TABLE IF EXISTS `usu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usu` (
  `ID_USU` int NOT NULL AUTO_INCREMENT,
  `CPF_USU` bigint NOT NULL,
  `nome_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `email_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pfp_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pfp_usu_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pfp_usu_img` varbinary(255) DEFAULT NULL,
  `NASC_USU` date NOT NULL,
  `rua_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `NUM_CASA_USU` int NOT NULL,
  `bairro_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cidade_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `estado_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CEP_USU` bigint NOT NULL,
  `TELEFONE_USU` bigint NOT NULL,
  `acc_type_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `situacao_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `SENHA_USU` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `SALT` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `COMPLEMENTO_USU` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `recover_usu` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `LB_COINS_USU` float(10,2) NOT NULL,
  PRIMARY KEY (`ID_USU`),
  KEY `idx_cpf_usu` (`CPF_USU`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usu`
--

LOCK TABLES `usu` WRITE;
/*!40000 ALTER TABLE `usu` DISABLE KEYS */;
INSERT INTO `usu` VALUES (50,62115264070,'Sandra Bruna Heloisa das Neves','sandrabrunadasneves@limao.com.br','','','','0001-01-01','',0,'','','',0,4835512467,'PRATA','Ativo','Fidelidade','','','',NULL,462.57);
/*!40000 ALTER TABLE `usu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-10-12  3:18:27
