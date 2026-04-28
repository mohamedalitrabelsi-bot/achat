// =============================================================================
// JENKINSFILE — Projet achat (Spring Boot)
// Sprint 2 : Intégration Continue avec Jenkins
// Équipe : Omar BOUHDIDA, Mohamed Ali TRABELSI, Sadek Amine BEN OUAGHREM
// =============================================================================

pipeline {

    // -------------------------------------------------------------------------
    // Agent : Jenkins utilisera n'importe quel nœud disponible
    // -------------------------------------------------------------------------
    agent any

    // -------------------------------------------------------------------------
    // Outils Maven et JDK configurés dans Jenkins Global Tool Configuration
    // -------------------------------------------------------------------------
    tools {
        maven 'Maven'    // doit correspondre au nom configuré dans Jenkins
        jdk   'Java21'   // Java 17 requis pour SonarQube / Nexus
    }

    // -------------------------------------------------------------------------
    // Variables d'environnement globales du pipeline
    // -------------------------------------------------------------------------
    environment {
        // Nom de l'application
        APP_NAME        = "achat"
        // Version extraite depuis Maven (sera mise à jour dynamiquement)
        APP_VERSION     = "1.0"
        // Répertoire de sortie Maven
        TARGET_DIR      = "target"
        // URL du dépôt GitHub (public)
        GIT_REPO_URL    = "https://github.com/omar-bouhdida/achat.git"
        // Branche principale
        GIT_BRANCH      = "main"
        // Outil Maven configuré dans Jenkins (voir Global Tool Configuration)
        MAVEN_TOOL      = "Maven"
        // Java Home (optionnel si JAVA_HOME est déjà défini sur l'agent)
        // JAVA_HOME    = "/usr/lib/jvm/java-17-openjdk-amd64"

        // SonarQube
        SONAR_HOST_URL  = 'http://localhost:9000'
        SONAR_TOKEN     = 'sqa_9f1d97bdbe5d25ab52191c63bfe41f44d6471005'
    }

    // -------------------------------------------------------------------------
    // Options globales du pipeline
    // -------------------------------------------------------------------------
    options {
        // Durée maximale d'exécution : 30 minutes
        timeout(time: 30, unit: 'MINUTES')
        // Conserver les 10 dernières exécutions seulement
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Horodater chaque ligne de log
        timestamps()
        // Ne pas lancer deux builds simultanément sur la même branche
        disableConcurrentBuilds()
    }

    // -------------------------------------------------------------------------
    // Déclencheurs automatiques
    // -------------------------------------------------------------------------
    triggers {
        // Scrutation du dépôt Git toutes les 5 minutes (alternative au webhook)
        // Syntaxe cron Jenkins : H/5 * * * *
        // Pour utiliser un webhook GitHub à la place, commenter cette ligne
        // et configurer le webhook dans GitHub → Settings → Webhooks
        pollSCM('H/5 * * * *')
    }

    // -------------------------------------------------------------------------
    // STAGES — Étapes du pipeline CI
    // -------------------------------------------------------------------------
    stages {

        // ---------------------------------------------------------------------
        // STAGE 1 : Récupération du code source depuis GitHub
        // ---------------------------------------------------------------------
        stage('🔍 Checkout') {
            steps {
                echo "========================================="
                echo " STAGE 1 : Récupération du code source"
                echo " Dépôt  : ${GIT_REPO_URL}"
                echo " Branche: ${GIT_BRANCH}"
                echo "========================================="

                // Checkout automatique géré par Jenkins (SCM configuré dans le job)
                // Jenkins clone le dépôt dans le workspace courant
                checkout scm

                // Afficher les informations du dernier commit
                sh '''
                    echo "--- Informations Git ---"
                    git log -1 --pretty=format:"Commit : %H%nAuteur : %an%nDate   : %ad%nMessage: %s"
                    echo ""
                    echo "--- Fichiers du projet ---"
                    ls -la
                '''
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 2 : Vérification de l'environnement
        // ---------------------------------------------------------------------
        stage('🔧 Vérification Environnement') {
            steps {
                echo "========================================="
                echo " STAGE 2 : Vérification de l'environnement"
                echo "========================================="

                sh '''
                    echo "--- Version Java ---"
                    java -version

                    echo "--- Version Maven ---"
                    mvn -version

                    echo "--- Contenu du workspace ---"
                    ls -la

                    echo "--- Vérification pom.xml ---"
                    if [ -f "pom.xml" ]; then
                        echo "✅ pom.xml trouvé"
                        grep -E "<groupId>|<artifactId>|<version>" pom.xml | head -10
                    else
                        echo "❌ pom.xml introuvable !"
                        exit 1
                    fi
                '''
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 3 : Compilation du projet
        // ---------------------------------------------------------------------
        stage('🔨 Build (Compilation)') {
            steps {
                echo "========================================="
                echo " STAGE 3 : Compilation Maven"
                echo "========================================="

                // mvn clean compile :
                //   clean   → supprime le répertoire target/
                //   compile → compile les sources Java
                sh './mvnw clean compile -B'
                // -B = batch mode (pas d'interactivité, sortie lisible dans Jenkins)
            }
            post {
                success {
                    echo "✅ Compilation réussie"
                }
                failure {
                    echo "❌ Échec de la compilation — vérifier les erreurs ci-dessus"
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 4 : Exécution des tests unitaires
        // ---------------------------------------------------------------------
        stage('🧪 Tests Unitaires') {
            steps {
                echo "========================================="
                echo " STAGE 4 : Tests unitaires (JUnit/Maven)"
                echo "========================================="

                // mvn test : exécute tous les tests unitaires
                sh './mvnw test -B'
            }
            post {
                always {
                    // Publier les rapports de tests JUnit dans Jenkins
                    // Visible dans l'interface : "Test Results"
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
                success {
                    echo "✅ Tous les tests ont réussi"
                }
                failure {
                    echo "❌ Des tests ont échoué — consulter le rapport JUnit"
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 5 : Packaging de l'application
        // ---------------------------------------------------------------------
        stage('📦 Package') {
            steps {
                echo "========================================="
                echo " STAGE 5 : Packaging Maven (JAR)"
                echo "========================================="

                // mvn package :
                //   - compile + test + génère le JAR/WAR final
                //   -DskipTests : les tests ont déjà été exécutés au stage précédent
                sh './mvnw package -DskipTests -B'

                // Vérifier que le JAR a bien été créé
                sh '''
                    echo "--- Artefacts générés ---"
                    ls -lh target/*.jar 2>/dev/null || ls -lh target/*.war 2>/dev/null || echo "Aucun artefact .jar/.war trouvé"
                '''
            }
            post {
                success {
                    echo "✅ Package généré avec succès"
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 6 : Analyse qualité du code avec SonarQube
        // ---------------------------------------------------------------------
        stage('📊 SonarQube Analysis') {
            steps {
                echo "========================================="
                echo " STAGE 6 : Analyse SonarQube"
                echo " URL : ${SONAR_HOST_URL}"
                echo "========================================="

                // Lance l'analyse statique du code via le plugin Sonar Maven
                // -Dsonar.projectKey  : identifiant du projet dans SonarQube
                // -Dsonar.host.url    : adresse du serveur SonarQube
                // -Dsonar.login       : token d'authentification
                sh """
                    ./mvnw  sonar:sonar -B \
                        -Dsonar.projectKey=achat \
                        -Dsonar.host.url=http://172.18.0.140:9000 \
                        -Dsonar.login=sqa_9f1d97bdbe5d25ab52191c63bfe41f44d6471005
                """
            }
            post {
                success {
                    echo "✅ Analyse SonarQube terminée — consulter le dashboard : ${SONAR_HOST_URL}"
                }
                failure {
                    echo "❌ Échec de l'analyse SonarQube — vérifier le token et l'URL"
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 7 : Déploiement vers Nexus Repository
        // ---------------------------------------------------------------------
        stage('🚀 Deploy to Nexus') {
            steps {
                echo "========================================="
                echo " STAGE 7 : Déploiement vers Nexus"
                echo "========================================="

                // mvn clean deploy :
                //   - recompile, re-package et publie l'artefact vers Nexus
                //   - Les coordonnées du dépôt Nexus doivent être définies
                //     dans le pom.xml (<distributionManagement>) ou settings.xml
                sh './mvnw clean deploy -DskipTests -B'
            }
            post {
                success {
                    echo "✅ Artefact déployé avec succès sur Nexus"
                }
                failure {
                    echo "❌ Échec du déploiement Nexus — vérifier distributionManagement dans pom.xml"
                }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 8 : Archivage des artefacts
        // ---------------------------------------------------------------------
        stage('💾 Archivage') {
            steps {
                echo "========================================="
                echo " STAGE 8 : Archivage des artefacts Jenkins"
                echo "========================================="
            }
            post {
                always {
                    // Archiver le JAR/WAR dans Jenkins pour téléchargement ultérieur
                    archiveArtifacts artifacts: 'target/*.jar,target/*.war',
                                     allowEmptyArchive: true,
                                     fingerprint: true

                    echo "✅ Artefacts archivés dans Jenkins"
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // POST — Actions exécutées après tous les stages
    // -------------------------------------------------------------------------
    post {

        // Toujours exécuté, quel que soit le résultat
        always {
            echo "========================================="
            echo " FIN DU PIPELINE"
            echo " Statut : ${currentBuild.currentResult}"
            echo " Durée  : ${currentBuild.durationString}"
            echo "========================================="
        }

        // Exécuté uniquement si le pipeline réussit
        success {
            echo "🎉 Pipeline CI terminé avec SUCCÈS !"
            echo "   Le build #${BUILD_NUMBER} est prêt."
        }

        // Exécuté uniquement si le pipeline échoue
        failure {
            echo "🚨 Pipeline CI ÉCHOUÉ au build #${BUILD_NUMBER}"
            echo "   Consulter les logs pour identifier l'erreur."
        }

        // Exécuté si le build est instable (tests en échec)
        unstable {
            echo "⚠️  Pipeline INSTABLE — des tests ont échoué"
        }

        // Exécuté si le statut change par rapport au build précédent
        changed {
            echo "🔄 Le statut du pipeline a changé depuis le dernier build"
        }
    }
}
