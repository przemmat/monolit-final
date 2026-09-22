pipeline {
	agent any

	environment {
		DOCKER_IMAGE = "logistics-monolith:${BUILD_NUMBER}"
		METRICS_DIR = "${WORKSPACE}/metrics"
		METRICS_FILE = "${WORKSPACE}/metrics/build_metrics_monolith.csv"
		TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
	}

	stages {
		stage('Initialize Telemetry') {
			steps {
				sh '''
                    mkdir -p ${METRICS_DIR}
                    if [ ! -f ${METRICS_FILE} ]; then
                        echo "build_number,stage,duration_ms,peak_ram_mb,docker_image_size_mb" > ${METRICS_FILE}
                    fi
                '''
			}
		}

		stage('Compile') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    mvn clean compile

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    echo "${BUILD_NUMBER},compile,${DIFF},0,0" >> ${METRICS_FILE}
                '''
			}
		}

		stage('Run All Tests (Monolith Suite)') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    # Monitor pamięci RAM w tle (próbkowanie co 0.5s)
                    MEM_LOG="${METRICS_DIR}/test_mem.log"
                    rm -f ${MEM_LOG}
                    (while true; do
                        # Pobieranie zużycia pamięci RSS procesu Maven/Java
                        ps -o rss,command -C java | awk '{print $1}' | sort -nr | head -n1 >> ${MEM_LOG}
                        sleep 0.5
                    done) &
                    MONITOR_PID=$!

                    # Wykonanie testów
                    mvn test
                    TEST_STATUS=$?

                    # Zatrzymanie monitora pamięci
                    kill $MONITOR_PID || true

                    END=$(date +%s%3N)
                    DIFF=$((END - START))

                    # Obliczenie szczytowego RAM w MB
                    PEAK_KB=$(sort -nr ${MEM_LOG} 2>/dev/null | head -n1 || echo 0)
                    if [ -z "$PEAK_KB" ]; then PEAK_KB=0; fi
                    PEAK_MB=$(echo "scale=2; ${PEAK_KB} / 1024" | bc)

                    echo "${BUILD_NUMBER},tests,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}

                    if [ $TEST_STATUS -ne 0 ]; then
                        exit $TEST_STATUS
                    fi
                '''
			}
			post {
				always {
					junit 'target/surefire-reports/*.xml'
				}
			}
		}

		stage('Package Application') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    mvn package -DskipTests

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    echo "${BUILD_NUMBER},package,${DIFF},0,0" >> ${METRICS_FILE}
                '''
			}
		}

		stage('Build Docker Image') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    docker build -t ${DOCKER_IMAGE} .

                    END=$(date +%s%3N)
                    DIFF=$((END - START))

                    # Rozmiar obrazu w MB
                    IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                    IMG_MB=$(echo "scale=2; ${IMG_BYTES} / 1048576" | bc)

                    echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                '''
			}
		}
	}

	post {
		always {
			// Zachowanie pliku metryk jako artefaktu Jenkinsa
			archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
		}
	}
}