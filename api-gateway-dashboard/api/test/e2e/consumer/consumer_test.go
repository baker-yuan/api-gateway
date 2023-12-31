package consumer_test

import (
	"net/http"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
)

var _ = Describe("Consumer", func() {
	DescribeTable("test consumer curd",
		func(tc base.HttpTestCase) {
			base.RunTestCase(tc)
		},
		Entry("create consumer", base.HttpTestCase{
			Object: base.ManagerApiExpect(),
			Method: http.MethodPut,
			Path:   "/apisix/admin/consumers",
			Body: `{
				"username": "consumer_1",
				"plugins": {
					"limit-count": {
						"count": 2,
						"time_window": 60,
						"rejected_code": 503,
						"key": "remote_addr",
						"policy": "local"
					}
				},
				"desc": "test description"
			}`,
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   []string{"\"code\":0", "\"username\":\"consumer_1\""},
		}),
		Entry("get consumer #1", base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodGet,
			Path:         "/apisix/admin/consumers/consumer_1",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   "\"username\":\"consumer_1\"",
		}),
		Entry("update consumer", base.HttpTestCase{
			Object: base.ManagerApiExpect(),
			Method: http.MethodPut,
			Path:   "/apisix/admin/consumers/consumer_1",
			Body: `{
				"username": "consumer_1",
				"plugins": {
					"limit-count": {
						"count": 2,
						"time_window": 60,
						"rejected_code": 504,
						"key": "remote_addr",
						"policy": "local"
					}
				},
				"desc": "test description"
			}`,
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   []string{"\"code\":0", "\"username\":\"consumer_1\"", "\"rejected_code\":504"},
		}),
		Entry("get consumer #2", base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodGet,
			Path:         "/apisix/admin/consumers/consumer_1",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   "\"rejected_code\":504",
		}),
		Entry("delete consumer", base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodDelete,
			Path:         "/apisix/admin/consumers/consumer_1",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   "\"code\":0",
		}),
	)

	DescribeTable("test consumer curd exception",
		func(tc base.HttpTestCase) {
			base.RunTestCase(tc)
		},
		Entry("create consumer by POST method", base.HttpTestCase{
			Object: base.ManagerApiExpect(),
			Method: http.MethodPost,
			Path:   "/apisix/admin/consumers",
			Body: `{
				"username": "consumer_1",
				"plugins": {
					"limit-count": {
					"count": 2,
					"time_window": 60,
					"rejected_code": 503,
					"key": "remote_addr",
					"policy": "local"
					}
				},
				"desc": "test description"
			}`,
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusNotFound,
			ExpectBody:   "404 page not found",
		}),
		Entry("create consumer with not exist plugin", base.HttpTestCase{
			Object: base.ManagerApiExpect(),
			Method: http.MethodPut,
			Path:   "/apisix/admin/consumers",
			Body: `{
				"username": "jack",
				"plugins": {
					"key-authnotexist": {
						"key": "auth-one"
					}
				},
				"desc": "test description"
			}`,
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusBadRequest,
			ExpectBody:   "schema validate failed: schema not found, path: plugins.key-authnotexist",
		}),
		Entry("delete consumer (as delete not exist consumer)", base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodDelete,
			Path:         "/apisix/admin/consumers/test",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusNotFound,
		}),
	)
})
