package id_compatible_test

import (
	"io/ioutil"
	"net/http"
	"time"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
	"github.com/tidwall/gjson"
)

var _ = DescribeTable("Id Not In Body",
	func(f func()) {
		f()
	},
	Entry("make sure the route is not created", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.APISIXExpect(),
			Method:       http.MethodGet,
			Path:         "/hello",
			ExpectStatus: http.StatusNotFound,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("create route that has no ID in request body by admin api", func() {
		base.RunTestCase(base.HttpTestCase{
			Object: base.APISIXAdminAPIExpect(),
			Method: http.MethodPut,
			Path:   "/apisix/admin/routes/r1",
			Body: `{
				"name": "route1",
				"uri": "/hello",
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1980": 1
					}
				}
			}`,
			Headers:      map[string]string{"X-API-KEY": "edd1c9f034335f136f87ad84b625c8f1"},
			ExpectStatus: http.StatusCreated,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("verify that the route is available for manager api", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodGet,
			Path:         "/apisix/admin/routes/r1",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   `"id":"r1"`,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("hit the route just created", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.APISIXExpect(),
			Method:       http.MethodGet,
			Path:         "/hello",
			ExpectStatus: http.StatusOK,
			ExpectBody:   "hello world",
			Sleep:        base.SleepTime,
		})
	}),
	Entry("delete the route", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodDelete,
			Path:         "/apisix/admin/routes/r1",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
		})
	}),
	Entry("hit deleted route", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.APISIXExpect(),
			Method:       http.MethodGet,
			Path:         "/hello",
			ExpectStatus: http.StatusNotFound,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("create route that has no ID in request body by admin api (POST)", func() {
		base.RunTestCase(base.HttpTestCase{
			Object: base.APISIXAdminAPIExpect(),
			Method: http.MethodPost,
			Path:   "/apisix/admin/routes",
			Body: `{
				"uri": "/hello",
				"upstream": {
					"type": "roundrobin",
					"nodes": {
						"` + base.UpstreamIp + `:1980": 1
					}
				}
			}`,
			Headers:      map[string]string{"X-API-KEY": "edd1c9f034335f136f87ad84b625c8f1"},
			ExpectStatus: http.StatusCreated,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("verify that the route is available for manager api", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.ManagerApiExpect(),
			Method:       http.MethodGet,
			Path:         "/apisix/admin/routes",
			Headers:      map[string]string{"Authorization": base.GetToken()},
			ExpectStatus: http.StatusOK,
			ExpectBody:   `"uri":"/hello"`,
			Sleep:        base.SleepTime,
		})
	}),
	Entry("hit the route just created", func() {
		base.RunTestCase(base.HttpTestCase{
			Object:       base.APISIXExpect(),
			Method:       http.MethodGet,
			Path:         "/hello",
			ExpectStatus: http.StatusOK,
			ExpectBody:   "hello world",
			Sleep:        base.SleepTime,
		})
	}),
	Entry("clear the route", func() {
		time.Sleep(time.Duration(100) * time.Millisecond)
		request, _ := http.NewRequest("GET", base.ManagerAPIHost+"/apisix/admin/routes", nil)
		request.Header.Add("Authorization", base.GetToken())
		resp, err := http.DefaultClient.Do(request)
		Expect(err).To(BeNil())
		defer resp.Body.Close()
		respBody, _ := ioutil.ReadAll(resp.Body)
		list := gjson.Get(string(respBody), "data.rows").Value().([]interface{})
		for _, item := range list {
			route := item.(map[string]interface{})
			base.RunTestCase(base.HttpTestCase{
				Desc:         "delete the route",
				Object:       base.ManagerApiExpect(),
				Method:       http.MethodDelete,
				Path:         "/apisix/admin/routes/" + route["id"].(string),
				Headers:      map[string]string{"Authorization": base.GetToken()},
				ExpectStatus: http.StatusOK,
			})
		}
	}),
)
