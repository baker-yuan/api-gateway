package system_config

import (
	"reflect"
	"time"

	"github.com/apisix/manager-api/internal/core/entity"
	"github.com/apisix/manager-api/internal/core/store"
	"github.com/apisix/manager-api/internal/handler"
	"github.com/gin-gonic/gin"
	"github.com/shiningrush/droplet"
	"github.com/shiningrush/droplet/wrapper"
	wgin "github.com/shiningrush/droplet/wrapper/gin"
)

type Handler struct {
	systemConfig store.Interface
}

func NewHandler() (handler.RouteRegister, error) {
	return &Handler{
		systemConfig: store.GetStore(store.HubKeySystemConfig),
	}, nil
}

func (h *Handler) ApplyRoute(r *gin.Engine) {
	r.GET("/apisix/admin/system_config/:config_name", wgin.Wraps(h.Get, wrapper.InputType(reflect.TypeOf(GetInput{}))))
	r.POST("/apisix/admin/system_config", wgin.Wraps(h.Post, wrapper.InputType(reflect.TypeOf(entity.SystemConfig{}))))
	r.PUT("/apisix/admin/system_config", wgin.Wraps(h.Put, wrapper.InputType(reflect.TypeOf(entity.SystemConfig{}))))
	r.DELETE("/apisix/admin/system_config/:config_name", wgin.Wraps(h.Delete, wrapper.InputType(reflect.TypeOf(DeleteInput{}))))
}

type GetInput struct {
	ConfigName string `auto_read:"config_name,path" validate:"required"`
}

func (h *Handler) Get(c droplet.Context) (interface{}, error) {
	input := c.Input().(*GetInput)
	r, err := h.systemConfig.Get(c.Context(), input.ConfigName)

	if err != nil {
		return handler.SpecCodeResponse(err), err
	}

	return r, nil
}

func (h *Handler) Post(c droplet.Context) (interface{}, error) {
	input := c.Input().(*entity.SystemConfig)
	input.CreateTime = time.Now().Unix()
	input.UpdateTime = time.Now().Unix()

	// create
	res, err := h.systemConfig.Create(c.Context(), input)
	if err != nil {
		return handler.SpecCodeResponse(err), err
	}

	return res, nil
}

func (h *Handler) Put(c droplet.Context) (interface{}, error) {
	input := c.Input().(*entity.SystemConfig)
	input.UpdateTime = time.Now().Unix()

	// update
	res, err := h.systemConfig.Update(c.Context(), input, false)
	if err != nil {
		return handler.SpecCodeResponse(err), err
	}

	return res, nil
}

type DeleteInput struct {
	ConfigName string `auto_read:"config_name,path" validate:"required"`
}

func (h *Handler) Delete(c droplet.Context) (interface{}, error) {
	input := c.Input().(*DeleteInput)
	err := h.systemConfig.BatchDelete(c.Context(), []string{input.ConfigName})

	if err != nil {
		return handler.SpecCodeResponse(err), err
	}

	return nil, nil
}
