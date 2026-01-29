import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
vus: 200,          // concurrent users
duration: '30s',   // test duration
};
export default function () {
    const url = 'http://localhost:8083/orders';

    const payload = JSON.stringify({
        productId: 101,
        quantity: 1,
        paymentMethod: "UPI"
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'order created': (r) => r.status === 200 || r.status === 201,
    });

    sleep(1); // realistic user wait
}
