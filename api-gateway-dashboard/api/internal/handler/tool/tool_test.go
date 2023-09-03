package tool

import (
	"testing"

	"github.com/apisix/manager-api/internal/utils"
	"github.com/shiningrush/droplet"
	"github.com/stretchr/testify/assert"
)

func TestTool_Version(t *testing.T) {
	h := Handler{}
	ctx := droplet.NewContext()

	hash, version := utils.GetHashAndVersion()

	ret, err := h.Version(ctx)
	assert.Nil(t, err)
	assert.Equal(t, &InfoOutput{
		Hash:    hash,
		Version: version,
	}, ret)
}
