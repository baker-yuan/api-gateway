package healthz_test

import (
	"testing"
	"time"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
)

func TestHealthz(t *testing.T) {
	RunSpecs(t, "Health Suite")
}

var _ = BeforeSuite(func() {
	base.CleanAllResource()
	time.Sleep(base.SleepTime)
})
