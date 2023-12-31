package label_test

import (
	"net/http"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
)

var _ = DescribeTable("Test label",
	func(tc base.HttpTestCase) {
		base.RunTestCase(tc)
	},
	Entry("config route", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Path:   "/apisix/admin/routes/r1",
		Method: http.MethodPut,
		Body: `{
		 "name": "route1",
		 "uri": "/hello",
		 "labels": {
			 "build":"16",
			 "env":"production",
			 "version":"v2"
		 },
		 "upstream": {
			 "type": "roundrobin",
			 "nodes": [{
				 "host": "` + base.UpstreamIp + `",
				 "port": 1980,
				 "weight": 1
			 }]
		 }
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("create consumer", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Path:   "/apisix/admin/consumers/c1",
		Method: http.MethodPut,
		Body: `{
		 "username": "c1",
		 "plugins": {
			 "key-auth": {
				 "key": "auth-one"
			 }
		 },
		 "labels": {
			 "build":"16",
			 "env":"production",
			 "version":"v3"
		 },
		 "desc": "test description"
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("create upstream", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/upstreams/u1",
		Body: `{
		 "nodes": [{
			 "host": "` + base.UpstreamIp + `",
			 "port": 1980,
			 "weight": 1
		 }],
		 "labels": {
			 "build":"17",
			 "env":"production",
			 "version":"v2"
		 },
		 "type": "roundrobin"
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("create service", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPost,
		Path:   "/apisix/admin/services",
		Body: `{
		 "id": "s1",
		 "plugins": {
			 "limit-count": {
				 "count": 2,
				 "time_window": 60,
				 "rejected_code": 503,
				 "key": "remote_addr",
				 "policy": "local"
			 }
		 },
		 "upstream": {
			 "type": "roundrobin",
			 "nodes": [{
				 "host": "39.97.63.215",
				 "port": 80,
				 "weight": 1
			 }]
		 },
		 "labels": {
			 "build":"16",
			 "env":"production",
			 "version":"v2",
			 "extra": "test"
		 }
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("create plugin_config", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/plugin_configs/1",
		Body: `{
		 "plugins": {
			 "response-rewrite": {
				 "headers": {
					 "X-VERSION":"22.0"
				 }
			 }
		 },
		 "labels": {
			 "version": "v2",
			 "build":   "17",
			 "extra":   "test"
		 }
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("get route label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/route",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"env\":\"production\"},{\"version\":\"v2\"}",
		Sleep:        base.SleepTime,
	}),
	Entry("get consumer label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/consumer",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"env\":\"production\"},{\"version\":\"v3\"}",
	}),
	Entry("get upstream label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/upstream",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"17\"},{\"env\":\"production\"},{\"version\":\"v2\"}",
	}),
	Entry("get service label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/service",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"env\":\"production\"},{\"extra\":\"test\"},{\"version\":\"v2\"}",
	}),
	Entry("get plugin_config label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/plugin_config",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"17\"},{\"extra\":\"test\"},{\"version\":\"v2\"}",
	}),
	Entry("update plugin_config", base.HttpTestCase{
		Object: base.ManagerApiExpect(),
		Method: http.MethodPut,
		Path:   "/apisix/admin/plugin_configs/1",
		Body: `{
		 "plugins": {
			 "response-rewrite": {
				 "headers": {
					 "X-VERSION":"22.0"
				 }
			 }
		 },
		 "labels": {
			 "version": "v3",
			 "build":   "16",
			 "extra":   "test"
		 }
	 }`,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("get plugin_config label again to verify update", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/plugin_config",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"extra\":\"test\"},{\"version\":\"v3\"}",
		Sleep:        base.SleepTime,
	}),
	Entry("get all label", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"},{\"env\":\"production\"},{\"extra\":\"test\"},{\"version\":\"v2\"},{\"version\":\"v3\"}",
	}),
	Entry("get label with page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Query:        "page=1&page_size=1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"}",
	}),
	Entry("get label with page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Query:        "page=3&page_size=1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"env\":\"production\"}",
	}),
	Entry("get labels (key = build)", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"}",
	}),
	Entry("get labels with the same key (key = build)", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build:16,build:17",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"}",
	}),
	Entry("get labels (key = build) with page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build&page=2&page_size=1",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"17\"}",
	}),
	Entry("get labels with same key (key = build) and page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build:16,build:17&page=1&page_size=2",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"}",
	}),
	Entry("get labels with same key (key = build) and page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build:16,build:17&page=2&page_size=1",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"17\"}",
	}),
	Entry("get labels (key = build && env = production)", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build,env:production",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"},{\"env\":\"production\"}",
	}),
	Entry("get labels (build=16 | 17 and env = production)", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build:16,build:17,env:production",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"build\":\"16\"},{\"build\":\"17\"},{\"env\":\"production\"}",
	}),
	Entry("get labels (key = build && env = production) with page", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodGet,
		Headers:      map[string]string{"Authorization": base.GetToken()},
		Query:        "label=build,env:production&page=3&page_size=1",
		Path:         "/apisix/admin/labels/all",
		ExpectStatus: http.StatusOK,
		ExpectBody:   "{\"env\":\"production\"}",
	}),
	Entry("delete route", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/routes/r1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("delete consumer", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/consumers/c1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("delete service", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/services/s1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("delete upstream", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/upstreams/u1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
	Entry("delete plugin_config", base.HttpTestCase{
		Object:       base.ManagerApiExpect(),
		Method:       http.MethodDelete,
		Path:         "/apisix/admin/plugin_configs/1",
		Headers:      map[string]string{"Authorization": base.GetToken()},
		ExpectStatus: http.StatusOK,
	}),
)
