package route_test

import (
	"net/http"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
)

var _ = DescribeTable("route with plugin proxy rewrite",
	func(tc base.HttpTestCase) {
		base.RunTestCase(tc)
	},
	Entry("make sure the route is not created", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         "/hello",
		ExpectStatus: http.StatusNotFound,
		ExpectBody:   `{"error_msg":"404 Route Not Found"}`,
	}),
	Entry("create route that will rewrite host and uri", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/routes/r1",
		Body: `{
				"name": "route1",
				"uri": "/hello",
				"plugins": {
					"proxy-rewrite": {
						"uri": "/plugin_proxy_rewrite",
						"host": "test.com"
					}
				},
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1982": 1
					}
				}
			}`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("verify route that rewrite host and uri", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         "/hello",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "uri: /plugin_proxy_rewrite\nhost: test.com",
		Sleep:        base.SleepTime,
	}),
	Entry("update route that will rewrite headers", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/routes/r1",
		Body: `{
				"name": "route1",
				"uri": "/hello",
				"plugins": {
					"proxy-rewrite": {
						"uri": "/uri/plugin_proxy_rewrite",
						"headers": {
							"X-Api-Version": "v2"
						}
					}
				},
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1982": 1
					}
				}
			}`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("verify route that rewrite headers", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         "/hello",
		Headers:      map[string]string{"X-Api-Version": "v1"},
		ExpectStatus: http.StatusOK,
		ExpectBody:   "x-api-version: v2",
		Sleep:        base.SleepTime,
	}),
	Entry("update route using regex_uri", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/routes/r1",
		Body: `{
				"name": "route1",
				"uri": "/test/*",
				"plugins": {
					"proxy-rewrite": {
						"regex_uri": ["^/test/(.*)/(.*)/(.*)", "/$1_$2_$3"]
					}
				},
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1982": 1
					}
				}
			}`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("verify route that using regex_uri", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         `/test/plugin/proxy/rewrite`,
		ExpectStatus: http.StatusOK,
		ExpectBody:   "uri: /plugin_proxy_rewrite",
		Sleep:        base.SleepTime,
	}),
	Entry("update route that will rewrite args", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/routes/r1",
		Body: `{
				"name": "route1",
				"uri": "/hello",
				"plugins": {
					"proxy-rewrite": {
						"uri": "/plugin_proxy_rewrite_args?name=api6"
					}
				},
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1982": 1
					}
				}
			}`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("verify route that rewrite args", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         `/hello`,
		Query:        "name=api7",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "uri: /plugin_proxy_rewrite_args\nname: api6",
		Sleep:        base.SleepTime,
	}),
	Entry("delete route", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/routes/r1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
		Sleep:        base.SleepTime,
	}),
	Entry("make sure the route deleted", base.HttpTestCase{
		Object:       base.APISIXExpect(),
		Method:       http.MethodGet,
		Path:         "/hello",
		ExpectStatus: http.StatusNotFound,
		ExpectBody:   `{"error_msg":"404 Route Not Found"}`,
		Sleep:        base.SleepTime,
	}),
)
