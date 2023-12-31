package id_compatible_test

import (
	"testing"
	"time"

	"github.com/apisix/manager-api/test/e2e/base"
	. "github.com/onsi/ginkgo/v2"
	. "github.com/onsi/gomega"
)

func TestIdCompatible(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Id Compatible Suite")
}

var _ = BeforeSuite(func() {
	base.CleanAllResource()
	time.Sleep(base.SleepTime)
})
