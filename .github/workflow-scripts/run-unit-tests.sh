cd dev
chmod +x gradlew

echo "Running gradle test and testReport tasks.  This will take approx. 30 minutes."

# Redirect stdout to log file that will get archived if any tests fail
mkdir gradle && touch unit.gradle.log
./gradlew --continue cnf:initialize testResults > gradle/unit.gradle.log

# Gradle testResults task will save off results in generated.properties ensure that file exists otherwise fail
if [[ ! -f "generated.properties" ]]; then
    echo "generated.properties file does not exist unit testing likely failed to complete"
    exit 1
fi 

total=$(cat generated.properties | grep tests.total.all | sed 's/[^0-9]*//g')
successful=$(cat generated.properties | grep tests.total.successful | sed 's/[^0-9]*//g')
failed=$(cat generated.properties | grep tests.total.failed | sed 's/[^0-9]*//g')
skipped=$(cat generated.properties | grep tests.total.skipped | sed 's/[^0-9]*//g')

echo -e "[Final Test Results] \n total:$total successful:$successful failed:$failed skipped:$skipped"

if [ $failed -gt 0 ]; then
    echo "::set-output name=status::failure"
else
    echo "::set-output name=status::success"
fi