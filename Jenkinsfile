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

                    mvn clean compile &
                    MVN_PID=$!
                    PEAK_KB=0
                    while kill -0 $MVN_PID 2>/dev/null; do
                        CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v p=$MVN_PID '$1==p || $2==p {sum+=$3} END {print sum+0}')
                        if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                        sleep 0.2
                    done
                    wait $MVN_PID

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    PEAK_MB=$(awk "BEGIN {printf \\"%.2f\\", ${PEAK_KB}/1024}")
                    echo "${BUILD_NUMBER},compile,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                '''
			}
		}

		stage('Run All Tests (Monolith Suite)') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    mvn test &
                    MVN_PID=$!
                    PEAK_KB=0
                    while kill -0 $MVN_PID 2>/dev/null; do
                        CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v p=$MVN_PID '$1==p || $2==p {sum+=$3} END {print sum+0}')
                        if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                        sleep 0.2
                    done
                    wait $MVN_PID
                    TEST_STATUS=$?

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    PEAK_MB=$(awk "BEGIN {printf \\"%.2f\\", ${PEAK_KB}/1024}")

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

                    mvn package -DskipTests &
                    MVN_PID=$!
                    PEAK_KB=0
                    while kill -0 $MVN_PID 2>/dev/null; do
                        CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v p=$MVN_PID '$1==p || $2==p {sum+=$3} END {print sum+0}')
                        if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                        sleep 0.2
                    done
                    wait $MVN_PID

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    PEAK_MB=$(awk "BEGIN {printf \\"%.2f\\", ${PEAK_KB}/1024}")
                    echo "${BUILD_NUMBER},package,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
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

                    IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                    IMG_MB=$(awk "BEGIN {printf \\"%.2f\\", ${IMG_BYTES}/1048576}")

                    echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                '''
			}
		}
	}

	post {
		always {
			archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
		}
	}
}