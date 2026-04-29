// =============================================================================
// JENKINSFILE — Projet achat (Spring Boot)
// Sprint 2 : Intégration Continue avec Jenkins
// Équipe : Omar BOUHDIDA, Mohamed Ali TRABELSI, Sadek Amine BEN OUAGHREM
// =============================================================================

pipeline {

    agent any

    tools {
        maven 'Maven'
        jdk   'Java21'
    }

    environment {
        APP_NAME        = "achat"
        APP_VERSION     = "1.0"
        TARGET_DIR      = "target"
        GIT_REPO_URL    = "https://github.com/omar-bouhdida/achat.git"
        GIT_BRANCH      = "main"
        MAVEN_TOOL      = "Maven"
        SONAR_HOST_URL  = 'http://localhost:9000'
        SONAR_TOKEN     = 'sqa_9f1d97bdbe5d25ab52191c63bfe41f44d6471005'
        DOCKER_IMAGE    = "dali8795/achat"
        DOCKER_TAG      = "1.0"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        disableConcurrentBuilds()
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {

        // ---------------------------------------------------------------------
        // STAGE 1 : Récupération du code source
        // ---------------------------------------------------------------------
        stage('🔍 Checkout') {
            steps {
                echo "========================================="
                echo " STAGE 1 : Récupération du code source"
                echo " Dépôt  : ${GIT_REPO_URL}"
                echo " Branche: ${GIT_BRANCH}"
                echo "========================================="
                checkout scm
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

                    echo "--- Version Docker ---"
                    docker --version

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
        // STAGE 3 : Compilation
        // ---------------------------------------------------------------------
        stage('🔨 Build (Compilation)') {
            steps {
                echo "========================================="
                echo " STAGE 3 : Compilation Maven"
                echo "========================================="
                sh './mvnw clean compile -B'
            }
            post {
                success { echo "✅ Compilation réussie" }
                failure { echo "❌ Échec de la compilation" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 4 : Tests Unitaires
        // ---------------------------------------------------------------------
        stage('🧪 Tests Unitaires') {
            steps {
                echo "========================================="
                echo " STAGE 4 : Tests unitaires (JUnit/Maven)"
                echo "========================================="
                sh './mvnw test -B'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
                success { echo "✅ Tous les tests ont réussi" }
                failure { echo "❌ Des tests ont échoué" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 5 : Package
        // ---------------------------------------------------------------------
        stage('📦 Package') {
            steps {
                echo "========================================="
                echo " STAGE 5 : Packaging Maven (JAR)"
                echo "========================================="
                sh './mvnw package -DskipTests -B'
                sh '''
                    echo "--- Artefacts générés ---"
                    ls -lh target/*.jar 2>/dev/null || echo "Aucun artefact .jar trouvé"
                '''
            }
            post {
                success { echo "✅ Package généré avec succès" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 6 : SonarQube
        // ---------------------------------------------------------------------
        stage('📊 SonarQube Analysis') {
            steps {
                echo "========================================="
                echo " STAGE 6 : Analyse SonarQube"
                echo " URL : ${SONAR_HOST_URL}"
                echo "========================================="
                sh """
                    ./mvnw sonar:sonar -B \
                        -Dsonar.projectKey=achat \
                        -Dsonar.host.url=http://172.18.0.140:9000 \
                        -Dsonar.login=sqa_9f1d97bdbe5d25ab52191c63bfe41f44d6471005
                """
            }
            post {
                success { echo "✅ Analyse SonarQube terminée" }
                failure { echo "❌ Échec de l'analyse SonarQube" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 7 : Déploiement Nexus
        // ---------------------------------------------------------------------
        stage('🚀 Deploy to Nexus') {
            steps {
                echo "========================================="
                echo " STAGE 7 : Déploiement vers Nexus"
                echo "========================================="
                sh './mvnw clean deploy -DskipTests -B'
            }
            post {
                success { echo "✅ Artefact déployé avec succès sur Nexus" }
                failure { echo "❌ Échec du déploiement Nexus" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 8 : Build Image Docker
        // ---------------------------------------------------------------------
        stage('🐳 Build Docker Image') {
            steps {
                echo "========================================="
                echo " STAGE 8 : Build de l image Docker"
                echo "========================================="
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
            post {
                success { echo "✅ Image Docker construite avec succès" }
                failure { echo "❌ Échec du build Docker" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 9 : Push vers DockerHub
        // ---------------------------------------------------------------------
        stage('📤 Push Docker Image') {
            steps {
                echo "========================================="
                echo " STAGE 9 : Push vers DockerHub"
                echo "========================================="
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        docker login -u $DOCKER_USER -p $DOCKER_PASS
                        docker push dali8795/achat:1.0
                    '''
                }
            }
            post {
                success { echo "✅ Image poussée sur DockerHub" }
                failure { echo "❌ Échec du push DockerHub" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 10 : Docker Compose
        // ---------------------------------------------------------------------
        stage('🚀 Docker Compose Up') {
            steps {
                echo "========================================="
                echo " STAGE 10 : Lancement Docker Compose"
                echo "========================================="
                sh 'docker compose up -d'
            }
            post {
                success { echo "✅ Application lancée avec Docker Compose" }
                failure { echo "❌ Échec Docker Compose" }
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 11 : Archivage
        // ---------------------------------------------------------------------
        stage('💾 Archivage') {
            steps {
                echo "========================================="
                echo " STAGE 11 : Archivage des artefacts Jenkins"
                echo "========================================="
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/*.jar,target/*.war',
                                     allowEmptyArchive: true,
                                     fingerprint: true
                    echo "✅ Artefacts archivés dans Jenkins"
                }
            }
        }
    }

    post {
        always {
            echo "========================================="
            echo " FIN DU PIPELINE"
            echo " Statut : ${currentBuild.currentResult}"
            echo " Durée  : ${currentBuild.durationString}"
            echo "========================================="
        }
        success {
            echo "🎉 Pipeline CI/CD terminé avec SUCCÈS !"
            echo "   Le build #${BUILD_NUMBER} est prêt."
        }
        failure {
            echo "🚨 Pipeline CI/CD ÉCHOUÉ au build #${BUILD_NUMBER}"
            echo "   Consulter les logs pour identifier l'erreur."
        }
        unstable {
            echo "⚠️  Pipeline INSTABLE — des tests ont échoué"
        }
        changed {
            echo "🔄 Le statut du pipeline a changé depuis le dernier build"
        }
    }
}
