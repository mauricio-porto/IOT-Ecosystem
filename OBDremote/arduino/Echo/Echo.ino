// ---------------------------------------------------------------------------
// 
// ---------------------------------------------------------------------------


void setup() {
  Serial.begin(9600); // Open serial monitor at 9600 baud to send info via Bluetooth.
  Serial.setTimeout(500);
}

void loop() {
  char line[100];
  if (Serial.available() > 0) {
    Serial.readBytes(line, 100);
    Serial.println(line);
  }
}

