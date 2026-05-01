const http = require('http');

function test(userType) {
  const options = {
    hostname: 'localhost',
    port: 3000,
    path: `/api/leaderboard?state=maharashtra&userType=${userType}`,
    method: 'GET'
  };

  const req = http.request(options, (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
      console.log(`\n--- RESULTS FOR ${userType} ---`);
      console.log(data);
    });
  });

  req.on('error', (e) => { console.error(e); });
  req.end();
}

test('INDIVIDUAL');
test('INSTITUTION');
